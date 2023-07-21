#version 150
// This float value should be defined from the compiling code.
// #define SCALE [2, 3, 4].0

#define SCALE 2.0
#ifdef GL_ES
#define PRECISION mediump
precision PRECISION float;
precision PRECISION int;
#else
#define PRECISION
#endif

uniform sampler2D u_texture;
uniform sampler2D u_lut;
uniform vec2 u_textureSize;

in vec4 v_texCoord[4];

const mat3 YUV_MATRIX = mat3(0.299, 0.587, 0.114, -0.169, -0.331, 0.5, 0.5, -0.419, -0.081);
const vec3 YUV_THRESHOLD = vec3(48.0/255.0, 7.0/255.0, 6.0/255.0);
const vec3 YUV_OFFSET = vec3(0, 0.5, 0.5);

out vec4 fragColor;

bool diff(vec3 yuv1, vec3 yuv2) {
    return any(greaterThan(abs((yuv1 + YUV_OFFSET) - (yuv2 + YUV_OFFSET)), YUV_THRESHOLD));
}

mat3 transpose(mat3 val) {
    mat3 result;
    result[0][1] = val[1][0];
    result[0][2] = val[2][0];
    result[1][0] = val[0][1];
    result[1][2] = val[2][1];
    result[2][0] = val[0][2];
    result[2][1] = val[1][2];
    return result;
}

void main() {
    vec2 fp = fract(v_texCoord[0].xy * u_textureSize);
    vec2 quad = sign(-0.5 + fp);
    mat3 yuv = transpose(YUV_MATRIX);

    float dx = v_texCoord[0].z;
    float dy = v_texCoord[0].w;
    vec3 p1  = texture2D(u_texture, v_texCoord[0].xy).rgb;
    vec3 p2  = texture2D(u_texture, v_texCoord[0].xy + vec2(dx, dy) * quad).rgb;
    vec3 p3  = texture2D(u_texture, v_texCoord[0].xy + vec2(dx, 0) * quad).rgb;
    vec3 p4  = texture2D(u_texture, v_texCoord[0].xy + vec2(0, dy) * quad).rgb;
    // Use mat4 instead of mat4x3 here to support GLES.
    mat4 pixels = mat4(vec4(p1, 0.0), vec4(p2, 0.0), vec4(p3, 0.0), vec4(p4, 0.0));

    vec3 w1  = yuv * texture2D(u_texture, v_texCoord[1].xw).rgb;
    vec3 w2  = yuv * texture2D(u_texture, v_texCoord[1].yw).rgb;
    vec3 w3  = yuv * texture2D(u_texture, v_texCoord[1].zw).rgb;

    vec3 w4  = yuv * texture2D(u_texture, v_texCoord[2].xw).rgb;
    vec3 w5  = yuv * p1;
    vec3 w6  = yuv * texture2D(u_texture, v_texCoord[2].zw).rgb;

    vec3 w7  = yuv * texture2D(u_texture, v_texCoord[3].xw).rgb;
    vec3 w8  = yuv * texture2D(u_texture, v_texCoord[3].yw).rgb;
    vec3 w9  = yuv * texture2D(u_texture, v_texCoord[3].zw).rgb;

    bvec3 pattern[3];
    pattern[0] =  bvec3(diff(w5, w1), diff(w5, w2), diff(w5, w3));
    pattern[1] =  bvec3(diff(w5, w4), false       , diff(w5, w6));
    pattern[2] =  bvec3(diff(w5, w7), diff(w5, w8), diff(w5, w9));
    bvec4 cross = bvec4(diff(w4, w2), diff(w2, w6), diff(w8, w4), diff(w6, w8));

    vec2 index;
    index.x = dot(vec3(pattern[0]), vec3(1, 2, 4)) +
    dot(vec3(pattern[1]), vec3(8, 0, 16)) +
    dot(vec3(pattern[2]), vec3(32, 64, 128));
    index.y = dot(vec4(cross), vec4(1, 2, 4, 8)) * (SCALE * SCALE) +
    dot(floor(fp * SCALE), vec2(1.0, SCALE));

    vec2 step = vec2(1.0) / vec2(256.0, 16.0 * (SCALE * SCALE));
    vec2 offset = step / vec2(2.0);
    vec4 weights = texture2D(u_lut, index * step + offset);
    float sum = dot(weights, vec4(1));
    vec3 res = (pixels * (weights / sum)).rgb;

    fragColor.rgb = res;
}