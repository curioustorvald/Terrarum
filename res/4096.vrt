varying vec2 texcoord;

void main(void) { // fairly usual fullscreen quad setup //
   vec2 corners = sign(gl_Vertex.xy);
   texcoord = 0.5 * corners + vec2(0.5);
   gl_Position = vec4(corners, 0.0, 1.0);
}