#version 150
#ifdef GL_ES
    precision mediump float;
#endif
#define SCALE 1.0

in vec4 a_position;
in vec2 a_texCoord0;

uniform mat4 u_projTrans;

out vec2 u_textureSize;
out vec4 v_texCoord[4];

void main() {
    gl_Position = u_projTrans * a_position / SCALE;

    vec2 ps = 1.0/u_textureSize;
    float dx = ps.x;
    float dy = ps.y;

    vec2 a_texCoord00 = a_texCoord0 / SCALE;

    //   +----+----+----+
    //   |    |    |    |
    //   | w1 | w2 | w3 |
    //   +----+----+----+
    //   |    |    |    |
    //   | w4 | w5 | w6 |
    //   +----+----+----+
    //   |    |    |    |
    //   | w7 | w8 | w9 |
    //   +----+----+----+

    v_texCoord[0].zw = ps;
    v_texCoord[0].xy = a_texCoord00.xy;
    v_texCoord[1] = a_texCoord00.xxxy + vec4(-dx, 0, dx, -dy); //  w1 | w2 | w3
    v_texCoord[2] = a_texCoord00.xxxy + vec4(-dx, 0, dx,   0); //  w4 | w5 | w6
    v_texCoord[3] = a_texCoord00.xxxy + vec4(-dx, 0, dx,  dy); //  w7 | w8 | w9
}