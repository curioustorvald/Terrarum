
in vec4 a_position;
in vec4 a_color;
in vec4 a_generic;
in vec2 a_texCoord0;

uniform mat4 u_projTrans;

out vec4 v_color;
out vec2 v_texCoords;
out vec4 v_generic;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    v_generic = a_generic;
    gl_Position = u_projTrans * a_position;
}