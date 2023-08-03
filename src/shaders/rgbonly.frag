#version 150
in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;

const vec2 boolean = vec2(0.0, 1.0);

out vec4 fragColor;

void main(void) {
    fragColor = texture(u_texture, v_texCoords) * boolean.yyyx + boolean.xxxy;
}