#version 150
#ifdef GL_ES
    precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

void main(void) {
    vec4 inColor = texture2D(u_texture, v_texCoords);
    ivec4 bytes = ivec4(255.0 * inColor);
    ivec4 mask = ivec4(0x55);
    fragColor = (bytes ^ mask) / 255.0;
}