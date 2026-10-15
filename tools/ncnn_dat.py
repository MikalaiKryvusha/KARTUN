"""ncnn_dat.py - makes spandrel's DAT (Dual Aggregation Transformer: UltraSharp V2, Nomos2 hq dat2, BHI dat2 ...)
exportable to ncnn by pnnx, without changing its math.

Why: ncnn keeps tensors in at most 4 dimensions and pnnx lowers torch.roll for one axis only, while DAT cuts the image into
attention windows with 6-D view/permute, adds the window mask through a 5-D view, shifts the map with a 2-axis roll, and
normalises with F.normalize (which lowers to aten::clamp_min, absent in ncnn). The functions below compute the same
tensors with <= 4-D ops, single-axis rolls and q / ||q||; batch must be 1 (the engine feeds one tile at a time).
Both attentions run as F.scaled_dot_product_attention, which pnnx lowers to ncnn's SDPA layer - it has a Vulkan kernel,
while ncnn's MatMul has none (108 MatMuls bouncing between the GPU and the CPU kept a 256 px image over 10 minutes on the
Titan). Window attention runs head by head with the windows as SDPA groups, so a head's position bias is one shared (N, N)
mask; the shifted-window mask is added to it at run time (a constant per head would be ~900 MB).
Trace at ONE fixed input size: the window mask and the padding are computed in Python from the size and become constants.
Research: researches/08_dat2_on_ncnn.md. Used by tools/convert-model.py (`patch(model)` returns how many modules changed).
[TESTED: 2026-10-15 · UltraSharp V2 patched vs original in PyTorch: max abs diff <= 1e-5 at 64 and 128 px; each attention
kind converted alone and checked on the PC's Vulkan GPU (<= 0.0044 on random input); the whole net at 128 px -> 126 SDPA,
no MatMul, ncnn (Vulkan, fp32 storage) vs PyTorch on a 128 px photo crop 66.6 dB. fp16 storage overflows (first Gemm ->
inf) - the engine keeps fixed-size nets in fp32. Linear -> 1x1 conv and no Squeeze/ExpandDims: 300 Convolution +
126 SDPA, 66.6 dB on the PC GPU. On the Titan's Mali: 13.9 s per 128 px tile but the picture is WRONG (16.1 dB) -
researches/08 section 9]
"""
import types

import torch
import torch.nn.functional as F
from spandrel.architectures.DAT.__arch import DAT as D

_roll = torch.roll


def roll1(x, s, d):
    return _roll(x, shifts=s, dims=d) if s else x


class LinearAsConv(torch.nn.Module):
    """nn.Linear over tokens (1, N, C) computed as a 1x1 convolution over (1, C, N, 1) - the same numbers.
    Why: pnnx turns a token Linear into ncnn Gemm, and ncnn's fp32 Vulkan Gemm on the Titan's Mali-G615 ran one
    16384x180x540 product in ~2.6 s (2026-10-15, layer timing on the device) - 144 of them made one 128 px tile take 267 s,
    while ncnn's Vulkan convolution is the fast path every ESRGAN model already runs on. Other ranks keep the Linear."""

    def __init__(self, lin):
        super().__init__()
        self.lin = lin
        self.conv = torch.nn.Conv2d(lin.in_features, lin.out_features, 1, bias=lin.bias is not None)
        with torch.no_grad():
            self.conv.weight.copy_(lin.weight[:, :, None, None])
            if lin.bias is not None:
                self.conv.bias.copy_(lin.bias)

    def forward(self, x):
        if x.dim() != 3:
            return self.lin(x)
        n, c = x.shape[1], x.shape[2]
        y = self.conv(x.transpose(1, 2).reshape(1, c, n, 1))
        return y.reshape(1, -1, n).transpose(1, 2)


def img2windows(img, H_sp, W_sp):
    _, C, H, W = img.shape
    x = img.permute(0, 2, 3, 1).reshape(H // H_sp, H_sp, W // W_sp, W_sp * C)  # Hn, H_sp, Wn, W_sp*C
    return x.permute(0, 2, 1, 3).reshape(-1, H_sp * W_sp, C)  # (Hn*Wn, N, C) - order Hn, Wn, H_sp, W_sp, C


def windows2img(x, H_sp, W_sp, H, W):
    C = x.shape[-1]
    x = x.reshape(H // H_sp, W // W_sp, H_sp, W_sp * C).permute(0, 2, 1, 3)  # Hn, H_sp, Wn, W_sp*C
    return x.reshape(1, H, W, C)


def sa_forward(self, qkv, H, W, mask=None):
    # Window attention as ncnn's SDPA layer (it has a Vulkan kernel; MatMul has none, and 108 MatMuls bouncing between
    # the GPU and the CPU made a 256 px image take over 10 minutes on the Titan). ncnn SDPA is 3-D (groups, seq, dim) with
    # a mask shared by all groups (2-D) or one per group (3-D), so each head runs separately with the windows as groups:
    # the position bias of a head is then ONE (N, N) mask shared by every window - a small constant.
    # qkv[i] would lower to Crop + Squeeze, and ncnn's Squeeze has no Vulkan kernel (a GPU->CPU->GPU trip each time):
    # a slice + reshape gives the same (1, L, C) with Vulkan layers only.
    _, _, L_, C = qkv.shape
    q, k, v = (qkv[i:i + 1].reshape(1, L_, C) for i in range(3))
    heads = self.num_heads
    d = C // heads
    N = self.H_sp * self.W_sp
    rpb = None
    if self.position_bias:
        pos = self.pos(self.rpe_biases)
        rpb = pos[self.relative_position_index.view(-1)].view(N, N, -1).permute(2, 0, 1).contiguous()  # (heads, N, N)

    def win(t, h):  # head h of (1, L, C) -> windows (nW, N, d); the head is a channel slice taken BEFORE windowing
        t = t[:, :, h * d:(h + 1) * d].transpose(-2, -1).contiguous().view(1, d, H, W)
        return img2windows(t, self.H_sp, self.W_sp)

    # Per head, every reshape keeps the forms the MatMul version already proved (66.5 dB): selecting a head out of a
    # (nW, heads, N, d) tensor and stacking heads back made pnnx mix the axes (2026-10-15: 0.81 max error per module).
    outs = []
    for h in range(heads):
        qw = win(q, h)
        m = None if rpb is None else rpb[h]  # (N, N)
        if mask is not None:
            # The shifted-window mask differs per window: stored per head it would be ~450 MB of constants, so it is
            # ADDED at run time. The zero comes from the windowed q, shaped (nW, N, 1): pnnx cannot fold it, and ncnn
            # broadcasts it along the last axis correctly (a (1, 1, 1) zero from the batch-1 tensor broadcast wrongly,
            # a scalar zero was folded away - both seen 2026-10-15).
            live_zero = qw[:, :, :1] * 0.0
            # left to right: (mask + live_zero) is already a run-time tensor, so pnnx cannot fold mask + bias into
            # one (nW, N, N) constant per head (the first try did: 54 of them, 906 MB)
            m = (mask + live_zero) if m is None else ((mask + live_zero) + m.reshape(1, N, N))  # (nW, N, N)
        o = F.scaled_dot_product_attention(qw, win(k, h), win(v, h), attn_mask=m, scale=self.scale)
        outs.append(windows2img(o, self.H_sp, self.W_sp, H, W))  # (1, H, W, d)
    return torch.cat(outs, dim=3)  # (1, H, W, C), channels [head][d] as the original


def asa_forward(self, x, H, W):
    _, L, C = x.shape
    qkv = self.qkv(x).reshape(L, 3, C).permute(1, 0, 2)  # 3, L, C
    v = qkv[2:3].reshape(L, C).transpose(0, 1).contiguous().view(1, C, H, W)
    m = max(self.split_size[0], self.split_size[1])
    pad_r, pad_b = (m - W % m) % m, (m - H % m) % m
    qkv = qkv.reshape(3, H, W, C).permute(0, 3, 1, 2)
    qkv = F.pad(qkv, (0, pad_r, 0, pad_b)).reshape(3, C, -1).transpose(-2, -1)  # 3, L', C
    _H, _W = pad_b + H, pad_r + W
    _L = _H * _W
    s0, s1 = self.shift_size
    if (self.rg_idx % 2 == 0 and self.b_idx > 0 and (self.b_idx - 2) % 4 == 0) or (self.rg_idx % 2 != 0 and self.b_idx % 4 == 0):
        q4 = qkv.reshape(3, _H, _W, C)
        qkv_0 = roll1(roll1(q4[:, :, :, : C // 2], -s0, 1), -s1, 2).reshape(3, 1, _L, C // 2)
        qkv_1 = roll1(roll1(q4[:, :, :, C // 2:], -s1, 1), -s0, 2).reshape(3, 1, _L, C // 2)
        if self.patches_resolution != _H or self.patches_resolution != _W:
            mt = self.calculate_mask(_H, _W, dtype=x.dtype)
            m0, m1 = mt[0].to(x.device), mt[1].to(x.device)
        else:
            m0, m1 = self.attn_mask_0, self.attn_mask_1
        x1 = roll1(roll1(self.attns[0](qkv_0, _H, _W, mask=m0), s0, 1), s1, 2)
        x2 = roll1(roll1(self.attns[1](qkv_1, _H, _W, mask=m1), s1, 1), s0, 2)
    else:
        q4 = qkv.reshape(3, 1, _L, C)  # 3, 1, L', C (reshape, not unsqueeze: ncnn's ExpandDims has no Vulkan kernel)
        x1 = self.attns[0](q4[:, :, :, : C // 2], _H, _W)
        x2 = self.attns[1](q4[:, :, :, C // 2:], _H, _W)
    x1 = x1[:, :H, :W, :].reshape(1, L, C // 2)
    x2 = x2[:, :H, :W, :].reshape(1, L, C // 2)
    attened_x = torch.cat([x1, x2], dim=2)
    conv_x = self.dwconv(v)
    channel_map = self.channel_interaction(conv_x).permute(0, 2, 3, 1).contiguous().view(1, 1, C)
    spatial_map = self.spatial_interaction(attened_x.transpose(-2, -1).contiguous().view(1, C, H, W))
    attened_x = attened_x * torch.sigmoid(channel_map)
    conv_x = torch.sigmoid(spatial_map) * conv_x
    conv_x = conv_x.permute(0, 2, 3, 1).contiguous().view(1, L, C)
    return self.proj_drop(self.proj(attened_x + conv_x))


def aca_forward(self, x, H, W):
    _, N, C = x.shape
    h = self.num_heads
    # Keep the batch axis (1) in front: pnnx swaps axes when a 4-D tensor starts with N (seen 2026-10-15: a Permute gave
    # (16384, 6, 30, 3) for (3, 6, 30, 16384) and the next Slice crashed ncnn). (1, N, 3C) -> (1, 3C, N), channels [3][h][d].
    t = self.qkv(x).transpose(1, 2)
    q = t[:, 0:C].reshape(h, C // h, N)  # = the original q.transpose(-2, -1), per head
    k = t[:, C:2 * C].reshape(h, C // h, N)
    v = t[:, 2 * C:].reshape(h, C // h, N)
    v_ = t[:, 2 * C:].reshape(1, C, H, W)
    # F.normalize = x / max(|x|, 1e-12) lowers to aten::clamp_min, which ncnn lacks; |x| is never near 1e-12 here
    q = q / torch.norm(q, p=2, dim=-1, keepdim=True)
    k = k / torch.norm(k, p=2, dim=-1, keepdim=True)
    # softmax(q k^T * temperature) v as ncnn SDPA: the per-head temperature moves into q, scale 1 (heads, d, N)
    o = F.scaled_dot_product_attention(q * self.temperature, k, v, scale=1.0)
    attened_x = o.reshape(1, C, N).transpose(1, 2)  # (heads, d, N) -> (1, N, C), channel order [h][d] as the original
    conv_x = self.dwconv(v_)
    channel_map = self.channel_interaction(attened_x.transpose(-2, -1).contiguous().view(1, C, H, W))
    spatial_map = self.spatial_interaction(conv_x).permute(0, 2, 3, 1).contiguous().view(1, N, 1)
    attened_x = attened_x * torch.sigmoid(spatial_map)
    conv_x = conv_x * torch.sigmoid(channel_map)
    conv_x = conv_x.permute(0, 2, 3, 1).contiguous().view(1, N, C)
    return self.proj_drop(self.proj(attened_x + conv_x))


def patch(model):
    D.img2windows = img2windows
    D.windows2img = windows2img
    n = 0
    # Every Linear -> LinearAsConv (collected first: the module tree must not change while it is walked).
    lins = [(parent, name, child) for parent in model.modules() for name, child in parent.named_children()
            if isinstance(child, torch.nn.Linear)]
    for parent, name, child in lins:
        setattr(parent, name, LinearAsConv(child))
        n += 1
    for mod in model.modules():
        name = type(mod).__name__
        if name == "Spatial_Attention":
            mod.forward = types.MethodType(sa_forward, mod); n += 1
        elif name == "Adaptive_Spatial_Attention":
            mod.forward = types.MethodType(asa_forward, mod); n += 1
        elif name == "Adaptive_Channel_Attention":
            mod.forward = types.MethodType(aca_forward, mod); n += 1
    return n
