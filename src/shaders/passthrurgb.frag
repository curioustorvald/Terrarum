#version 150
#ifdef GL_ES
    precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;

out vec4 fragColor;

void main(void) {
    fragColor = vec4(texture(u_texture, v_texCoords).rgb, 1.0);
}