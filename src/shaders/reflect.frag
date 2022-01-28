varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main(void) {
    vec4 color = texture2D(u_texture, vec2(v_texCoords.x, 1.0 - v_texCoords.y));
    vec4 alphamul = vec4(1.0, 1.0, 1.0, 0.5 * (1.0 - v_texCoords.y));
    gl_FragColor = color * alphamul;
}