#version 150

in vec4 a_position;
in vec2 a_texCoord0;

uniform vec2 u_textureSize;

out vec4 v_texCoord[4];

void main() {
    gl_Position = a_position;

    vec2 ps = 1.0/u_textureSize;
    float dx = ps.x;
    float dy = ps.y;

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
    v_texCoord[0].xy = a_texCoord0.xy;
    v_texCoord[1] = a_texCoord0.xxxy + vec4(-dx, 0, dx, -dy); //  w1 | w2 | w3
    v_texCoord[2] = a_texCoord0.xxxy + vec4(-dx, 0, dx,   0); //  w4 | w5 | w6
    v_texCoord[3] = a_texCoord0.xxxy + vec4(-dx, 0, dx,  dy); //  w7 | w8 | w9
}