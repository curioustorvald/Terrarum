#version 400


in vec4 v_color; // lightCol
in vec4 v_generic; // gamma values
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

const vec2 boolean = vec2(0.0, 1.0);

uniform vec4 shadeCol;

void main() {
    // vertex colour format:
    // rrrrrrMM ggggggLL bbbbbbAA aaaaaaaa
    // where:
    //    rrrrrr: 6-bit red component
    //    gggggg: 6-bit green component
    //    bbbbbb: 6-bit blue component
    //      MMLL: index to the rgbGammas
    //        AA: index to the aGammas
    vec4 cloudCol = v_color;
    float rgbGamma = v_generic.x;
    float aGamma = v_generic.y;
    vec4 gamma = v_generic.xxxy;

    vec4 range = vec4(vec3(min(rgbGamma, 1.0 / rgbGamma)), 1.0);
    vec4 offset = vec4(vec3(max(0.0, 1.0 - rgbGamma)), 0.0);

    // cloud colour format:
    // r: bw diffuse map, g: normal, b: normal, a: bw diffuse alpha
    vec4 inCol = texture(u_texture, v_texCoords);
    vec4 rawCol = range * pow(inCol, gamma) + offset;

    // do gradient mapping here
    vec4 outCol = fma(mix(shadeCol, cloudCol, rawCol.r), boolean.yyyx, rawCol * boolean.xxxy);

    fragColor = outCol * fma(cloudCol, boolean.xxxy, boolean.yyyx);
}