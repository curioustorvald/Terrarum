#version 150

in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;

uniform mat4 u_projTrans;

out vec4 v_color;
out vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_color.a = v_color.a * (255.0/254.0); // GDX will crush the alpha value to 0,2,4,6,8,10,...,254; see com.badlogic.gdx.utils.NumberUtils.intToFloatColor
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}