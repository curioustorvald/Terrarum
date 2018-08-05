local names = {
"layerWall",
"layerTerrain",
"layerWire",
"layerWallLowBits",
"layerTerrainLowBits",
"layerThermal",
"spawnX",
"spawnY",
"wallDamages",
"terrainDamages",
"globalLight",
"averageTemperature",
"generatorSeed",
"terrainArray",
"wallArray",
"wireArray",
"damageDataArray"
}

local valvar = {
"val",
"val",
"val",
"val",
"val",
"val",
"var",
"var",
"val",
"val",
"var",
"var",
"var",
"val",
"val",
"val",
"val"
}

local types = {
	"MapLayer",
"MapLayer",
"MapLayer",
"PairedMapLayer",
"PairedMapLayer",
"MapLayerFloat",
"Int",
"Int",
"HashMap<BlockAddress, BlockDamage>",
"HashMap<BlockAddress, BlockDamage>",
"Color",
"Float",
"Long",
"ByteArray",
"ByteArray",
"ByteArray",
"ByteArray"
}



for i = 1, #types do
	n = names[i]
	v = valvar[i]
	t = types[i]

	if (v == "val") then
		print(string.format("%s %s: %s; get() = baseworld.%s", v, n, t, n))
	else
		print(string.format("%s %s: %s; get() = baseworld.%s; set(v) { baseworld.%s = v }", v, n, t, n, n))
	end
end