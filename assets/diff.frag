#version 120
#ifdef GL_ES
    precision mediump float;
#endif
#extension GL_EXT_gpu_shader4 : enable


varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main(void) {
    vec4 inColor = texture2D(u_texture, v_texCoords);
    ivec4 bytes = ivec4(255.0 * inColor);
    ivec4 mask = ivec4(0x55);
    gl_FragColor = (bytes ^ mask) / 255.0;
}