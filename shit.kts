val list = intArrayOf(0,2,4,6,9,13,16,54,88)

val ID = 555

var low = 0
var high = list.size
var mid = -1

while (low < high) {
    mid = (low + high).ushr(1)

    if (list[mid] > ID)
        high = mid
    else
        low = mid + 1

    low
}

println("$low, $high, $mid")

val ll = arrayListOf(1,1,1,1,1)
ll.add(5, 8)
println(ll)

// take mid value? (except for ID < list[0])