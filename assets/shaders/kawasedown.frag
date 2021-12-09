#version 120
#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform vec2 halfpixel = vec2(0.0, 0.0);

vec2 twister = vec2(1.0, -1.0);

void main() {
    vec4 sum = texture2D(u_texture, v_texCoords) * 4.0;
    sum += texture2D(u_texture, v_texCoords - halfpixel);
    sum += texture2D(u_texture, v_texCoords + halfpixel);
    sum += texture2D(u_texture, v_texCoords - halfpixel * twister);
    sum += texture2D(u_texture, v_texCoords + halfpixel * twister);
    gl_FragColor = sum / 8.0;

//    gl_FragColor = texture2D(u_texture, v_texCoords);
}