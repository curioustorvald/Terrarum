#version 130
#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

vec2 boolean = vec2(0.0, 1.0);

void main() {
    vec4 inColor = v_color * (texture2D(u_texture, v_texCoords));

    vec4 divided = inColor / pow(inColor.aaaa, vec4(2.0));
    gl_FragColor = divided * boolean.yyyx + inColor * boolean.xxxy;
}