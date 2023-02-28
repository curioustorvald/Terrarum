#version 150
in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;

out vec4 fragColor;

void main(void) {
    vec4 color = texture(u_texture, vec2(v_texCoords.x, 1.0 - v_texCoords.y));
    vec4 alphamul = vec4(1.0, 1.0, 1.0, 0.5 * (1.0 - v_texCoords.y));
    fragColor = color * alphamul;
}