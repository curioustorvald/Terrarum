


in vec4 v_color; // lightCol
in vec4 v_generic; // [rgb gamma, a gamma, shadiness, 0]
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

const vec2 boolean = vec2(0.0, 1.0);

uniform vec4 shadeCol;
//uniform float shadiness = 1.0;

vec4 shadeVec = vec4(1.0 + 3.333 * v_generic.z, 1.0 + 3.333 * v_generic.z, 1.0 + 3.333 * v_generic.z, 1.0);

void main() {
    vec4 cloudCol = v_color;
    float rgbGamma = v_generic.x;
    float aGamma = v_generic.y;
    vec4 gamma = v_generic.xxxy;

    vec4 range = vec4(vec3(min(rgbGamma, 1.0 / rgbGamma)), 1.0);
    vec4 offset = vec4(vec3(max(0.0, 1.0 - rgbGamma)), 0.0);

    // cloud colour format:
    // r: bw diffuse map, g: normal, b: normal, a: bw diffuse alpha
    vec4 inCol = texture(u_texture, v_texCoords);
    vec4 rawCol0 = range * pow(inCol, gamma) + offset;
    vec4 rawCol = pow(rawCol0, shadeVec);

    // do gradient mapping here
    vec4 ccol = mix(shadeCol, cloudCol, rawCol.r);
    vec4 outCol = fma(ccol, boolean.yyyx, rawCol * boolean.xxxy);

    fragColor = outCol * fma(cloudCol, boolean.xxxy, boolean.yyyx);
}