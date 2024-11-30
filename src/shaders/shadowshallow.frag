
#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_color;
in vec4 v_generic;
in vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D u_wall;
out vec4 fragColor;

vec4 mult = vec4(0.0, 0.0, 0.0, 1.0);

void main() {
    vec4 backcol = texture(u_wall, v_texCoords);
    vec4 incol = texture(u_texture, v_texCoords);
    vec4 outcol = vec4(incol.rgb, backcol.a * incol.a);
    fragColor = mult * outcol;
}