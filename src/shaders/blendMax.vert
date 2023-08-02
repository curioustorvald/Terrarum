#version 150

in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;
in vec2 a_texCoord1;

uniform mat4 u_projTrans; // camera.combined

out vec4 v_color;
out vec2 v_texCoord0;
out vec2 v_texCoord1;

void main() {
    v_color = a_color;
    v_texCoord0 = a_texCoord0;
    v_texCoord1 = a_texCoord1;
    gl_Position = u_projTrans * a_position;
}