
in highp vec4 a_position;
in highp vec4 a_color;
in highp vec4 a_generic;
in highp vec2 a_texCoord0;

uniform mat4 u_projTrans;

out mediump vec4 v_color;
out highp vec2 v_texCoords;
out highp vec4 v_generic;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    v_generic = a_generic;
    gl_Position = u_projTrans * a_position;
}