#version 150

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in LOWP vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

void main() {
    fragColor = v_color * texture(u_texture, v_texCoords);
}