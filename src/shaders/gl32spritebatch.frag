
#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_color;
in vec4 v_generic;
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

void main() {
    fragColor = v_color * texture(u_texture, v_texCoords);
}