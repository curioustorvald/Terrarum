varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main(void) {
    vec3 alpha = texture2D(u_texture, v_texCoords).aaa;
    gl_FragColor = vec4(alpha, 1.0);
}