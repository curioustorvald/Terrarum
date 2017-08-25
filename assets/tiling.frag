#version 120
#ifdef GL_ES
    precision mediump float;
#endif

layout(origin_upper_left) in vec4 gl_FragCoord;


varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D tilesAtlas;
uniform sampler2D backgroundTexture;

uniform vec2 tileInAtlas = vec2(256, 256);
uniform vec2 atlasTexSize = vec2(4096, 4096);


uniform vec2 tileInDim; // vec2(tiles_in_horizontal, tiles_in_vertical)
uniform vec2 cameraTranslation = vec2(0, 0);
uniform float tileSizeInPx = 16;


uniform float tilemap[tileInDim.x * tileInDim.y]; // must be float array


void main() {

    vec2 pxCoord = gl_FragCoord.xy - cameraTranslation;
    vec2 pxCoordModTilesize = mod(pxCoord, tileSizeInPx);
    vec2 tileCoord = floor(pxCoord / tileCoord);

    int absoluteTileCoord = int(tileCoord.x + tileCoord.y * tileInDim.x);


    float tile = tilemap[absoluteTileCoord]; // sure it's integer at this point
    vec2 fragCoordInAtlas = vec2(
            tileSizeInPx * mod(tile, tileInAtlas.x)    + pxCoordModTilesize.x,
            tileSizeInPx * floor(tile / tileInAtlas.x) + pxCoordModTilesize.y
    );
    vec2 fragCoordUV = vec2(fragCoordInAtlas.x / atlasTexSize.x, 1 - fragCoordInAtlas.y / atlasTexSize.y);
    vec4 fragInAtlas = texture2D(tilesAtlas, fragCoordUV);


	gl_FragColor = fragInAtlas;
}
