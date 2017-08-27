#version 120
#ifdef GL_ES
    precision mediump float;
#endif
#extension GL_EXT_gpu_shader4 : enable

//layout(origin_upper_left) in vec4 gl_FragCoord; // commented; requires #version 150 or later
// gl_FragCoord is origin to bottom-left

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;


uniform vec2 screenDimension;
uniform vec2 tilesInAxes;

uniform ivec2 tilemapDimension;
uniform sampler2D tilemap; // MUST be RGBA8888

uniform sampler2D tilesAtlas;
uniform sampler2D backgroundTexture;

uniform ivec2 tileInAtlas = ivec2(256, 256);
uniform ivec2 atlasTexSize = ivec2(4096, 4096);


//uniform vec2 tileInDim; // vec2(tiles_in_horizontal, tiles_in_vertical)
uniform vec2 cameraTranslation = vec2(0, 0); // Y-flipped
uniform int tileSizeInPx = 16;



ivec2 getTileXY(int tileNumber) {
    return ivec2(tileNumber % int(tileInAtlas.x), tileNumber / int(tileInAtlas.x));
}

int getTileFromColor(vec4 color) {
    return int(color.b * 255) + (int(color.g * 255) * 256) + (int(color.r * 255) * 65536);
}

void main() {

    // READ THE FUCKING MANUAL, YOU DONKEY !! //
    // This code purposedly uses flipped fragcoord. //
    // Make sure you don't use gl_FragCoord unknowingly! //


    // default gl_FragCoord takes half-integer (represeting centre of the pixel) -- could be useful for phys solver?
    // This one, however, takes exact integer by rounding down. //
    vec2 overscannedScreenDimension = tilesInAxes * tileSizeInPx;
    vec2 flippedFragCoord = vec2(v_texCoords.x * screenDimension.x, (1 - v_texCoords.y) * screenDimension.y); // NO IVEC2!!

    vec2 pxCoord = flippedFragCoord.xy + cameraTranslation;

    mediump vec4 tileFromMap = texture2D(tilemap, flippedFragCoord / overscannedScreenDimension); // <- THE CULPRIT
    int tile = getTileFromColor(tileFromMap);


    ivec2 tileXY = getTileXY(tile);


    vec2 coordInTile = mod(pxCoord, tileSizeInPx) / tileSizeInPx; // 0..1 regardless of tile position in atlas

    highp vec2 singleTileSizeInUV = vec2(1) / tileInAtlas; // 0.00390625
    highp vec2 uvCoordForTile = coordInTile * singleTileSizeInUV; // 0..0.00390625 regardless of tile position in atlas

    highp vec2 uvCoordOffset = tileXY * singleTileSizeInUV; // where the tile starts in the atlas, using uv coord (0..1)

    highp vec2 finalUVCoordForTile = uvCoordForTile + uvCoordOffset;// where we should be actually looking for in atlas, using UV coord (0..1)


    gl_FragColor = texture2D(tilesAtlas, finalUVCoordForTile);
    //gl_FragColor = texture2D(tilemap, v_texCoords); // tilemap seems normal...
    //gl_FragColor = tileFromMap; // <- oh, THIS WAS THE CULPRIT!



	//gl_FragColor = fragInAtlas;
	//gl_FragColor = vec4((gl_FragCoord.xy / vec2(512, 512)), 0, 1.0);

	//vec4 atlascol = texture2D(tilesAtlas, v_texCoords);
	//vec4 tilemapcol = texture2D(tilemap, v_texCoords);

	//gl_FragColor = atlascol * tilemapcol;
	//gl_FragColor = vec4(v_texCoords.x, v_texCoords.y, 0, 1.0);
}
