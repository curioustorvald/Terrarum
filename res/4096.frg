varying vec2 texcoord;

uniform sampler2D renderTexture;
uniform mat4 Bayer;
uniform int pixelSize;

void main(void) {
   // create texture coordinates based on pixelSize //
   
   // vec2 discrete = (gl_FragCoord.xy + 0.001) / texcoord / pixelSize; //

   vec2 pixelSizeVec = vec2(float(pixelSize), float(pixelSize));

   vec2 discrete = (gl_FragCoord.xy + 0.001) / texcoord / pixelSizeVec;

   discrete = floor(discrete * texcoord) / discrete;

   vec3 color = texture2D(renderTexture, discrete).rgb;

   // increase contrast (Bayer matrix operation reduces it) //
   float contrast = 1.65;
   color = mix(vec3(0.5), color, contrast);

   // add Bayer matrix entry to current pixel //
   // vec2 entry = mod(gl_FragCoord.xy / pixelSizeVec, vec2(4, 4));
   
   // color.r = color.r + Bayer[int(entry.x)][int(entry.y)] / 17.0 - 0.5;
   // color.g = color.g + Bayer[int(entry.x)][int(entry.y)] / 17.0 - 0.5;
   // color.b = color.b + Bayer[int(entry.x)][int(entry.y)] / 17.0 - 0.5;

   // find nearest 8-bit color //
   color.r = floor(8.0 * color.r + 0.5) / 8.0;
   color.g = floor(8.0 * color.g + 0.5) / 8.0;
   color.b = floor(4.0 * color.b + 0.5) / 4.0;

   gl_FragColor = vec4(color, 1.0);
}