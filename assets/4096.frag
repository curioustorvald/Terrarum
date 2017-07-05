varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;


void main(void) {
    vec4 color = texture2D(u_texture, v_texCoords);
    color = floor(15.0 * color + 0.5) / 15.0;

    gl_FragColor = color;
}