
in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;

uniform vec4 ghostColour = vec4(1.0);

const vec2 boolean = vec2(0.0, 1.0);
const vec4 desaturate = vec4(0.2126, 0.7152, 0.0722, 0.0);
out vec4 fragColor;

void main(void) {
    vec4 incolour = texture(u_texture, v_texCoords);
    float lum = dot(incolour * desaturate, boolean.yyyx) * 0.5 + 0.5;

    fragColor = ghostColour * fma(vec4(lum), boolean.yyyx, incolour * boolean.xxxy);
}