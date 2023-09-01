#version 400

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in LOWP vec4 v_color; // lightCol.rgb + cloud's alpha
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

const vec2 boolean = vec2(0.0, 1.0);

uniform LOWP vec4 shadeCol;

const float rgbGammas[16] = float[](
0.2,
0.3,
0.4,
0.5,

0.7,
0.9,
1.1,
1.3,

1.7,
2.1,
2.5,
2.9,

3.7,
4.5,
5.3,
6.1
);

const float aGammas[4] = float[](
1.6,
2.0,
2.4,
2.8
);

void main() {
    // vertex colour format:
    // rrrrrrMM ggggggLL bbbbbbAA aaaaaaaa
    // where:
    //    rrrrrr: 6-bit red component
    //    gggggg: 6-bit green component
    //    bbbbbb: 6-bit blue component
    //      MMLL: index to the rgbGammas
    //        AA: index to the aGammas
    vec4 cloudCol = vec4(
        (int(v_color.r * 255) >> 2) * 4.0 / 255.0,
        (int(v_color.g * 255) >> 2) * 4.0 / 255.0,
        (int(v_color.b * 255) >> 2) * 4.0 / 255.0,
        v_color.a
    );
    float rgbGamma = rgbGammas[((int(v_color.r * 255) & 3) << 2) | (int(v_color.g * 255) & 3)];
    float aGamma = aGammas[int(v_color.b * 255) & 3];
    vec4 gamma = vec4(rgbGamma, rgbGamma, rgbGamma, aGamma);

    // cloud colour format:
    // r: bw diffuse map, g: normal, b: normal, a: bw diffuse alpha
    vec4 inCol = texture(u_texture, v_texCoords);
    vec4 rawCol = pow(inCol, gamma);

    // do gradient mapping here
    vec4 outCol = fma(mix(shadeCol, cloudCol, rawCol.r), boolean.yyyx, rawCol * boolean.xxxy);

    fragColor = outCol * fma(cloudCol, boolean.xxxy, boolean.yyyx);
}