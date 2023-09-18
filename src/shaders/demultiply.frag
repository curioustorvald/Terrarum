#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;

const vec2 boolean = vec2(0.0, 1.0);
out vec4 fragColor;

void main() {
    vec4 inColor = v_color * (texture(u_texture, v_texCoords));

    vec4 divided = inColor / pow(inColor.aaaa, vec4(2.0));
    fragColor = divided * boolean.yyyx + inColor * boolean.xxxy;
}