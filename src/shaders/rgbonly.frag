#version 150
in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;

vec2 boolean = vec2(0.0, 1.0);

out vec4 fragColor;

void main(void) {
    fragColor = texture2D(u_texture, v_texCoords).rgba * boolean.yyyx + boolean.xxxy;
}