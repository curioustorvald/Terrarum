// Polyfilling some functions from ECMAScript6+
if (!String.prototype.repeat) {
    String.prototype.repeat = function(count) {
        'use strict';
        if (this == null)
            throw new TypeError('can\'t convert ' + this + ' to object');

        var str = '' + this;
        // To convert string to integer.
        count = +count;
        // Check NaN
        if (count != count)
            count = 0;

        if (count < 0)
            throw new RangeError('repeat count must be non-negative');

        if (count == Infinity)
            throw new RangeError('repeat count must be less than infinity');

        count = Math.floor(count);
        if (str.length == 0 || count == 0)
            return '';

        // Ensuring count is a 31-bit integer allows us to heavily optimize the
        // main part. But anyway, most current (August 2014) browsers can't handle
        // strings 1 << 28 chars or longer, so:
        if (str.length * count >= 1 << 28)
            throw new RangeError('repeat count must not overflow maximum string size');

        var maxCount = str.length * count;
        count = Math.floor(Math.log(count) / Math.log(2));
        while (count) {
             str += str;
             count--;
        }
        str += str.substring(0, maxCount - str.length);
        return str;
    }
}
if (!String.prototype.startsWith) {
    Object.defineProperty(String.prototype, 'startsWith', {
        value: function(search, rawPos) {
            var pos = rawPos > 0 ? rawPos|0 : 0;
            return this.substring(pos, pos + search.length) === search;
        }
    });
}
if (!String.prototype.endsWith) {
	Object.defineProperty(String.prototype, 'endsWith', {
        value: function(search, this_len) {
                if (this_len === undefined || this_len > this.length) {
                this_len = this.length;
            }
            return this.substring(this_len - search.length, this_len) === search;
        }
    });
}
/**
 * String.prototype.replaceAll() polyfill
 * https://gomakethings.com/how-to-replace-a-section-of-a-string-with-another-one-with-vanilla-js/
 * @author Chris Ferdinandi
 * @license MIT
 */
if (!String.prototype.replaceAll) {
	String.prototype.replaceAll = function(str, newStr){

		// If a regex pattern
		if (Object.prototype.toString.call(str).toLowerCase() === '[object regexp]') {
			return this.replace(str, newStr);
		}

		// If a string
		return this.replace(new RegExp(str, 'g'), newStr);

	};
}
if (!Array.prototype.filter){
    Array.prototype.filter = function(func, thisArg) {
        'use strict';
        if ( ! ((typeof func === 'Function' || typeof func === 'function') && this) )
                throw new TypeError();

        var len = this.length >>> 0,
                res = new Array(len), // preallocate array
                t = this, c = 0, i = -1;

        var kValue;
        if (thisArg === undefined){
            while (++i !== len){
                // checks to see if the key was set
                if (i in this){
                    kValue = t[i]; // in case t is changed in callback
                    if (func(t[i], i, t)){
                        res[c++] = kValue;
                    }
                }
            }
        }
        else{
            while (++i !== len){
                // checks to see if the key was set
                if (i in this){
                    kValue = t[i];
                    if (func.call(thisArg, t[i], i, t)){
                        res[c++] = kValue;
                    }
                }
            }
        }

        res.length = c; // shrink down array to proper size
        return res;
    };
}
if (!String.prototype.padStart) {
    String.prototype.padStart = function(l, c) {
        return (this.length >= l) ? this : (c.repeat(l - this.length) + this);
    };
}
// Production steps of ECMA-262, Edition 5, 15.4.4.19
// Reference: http://es5.github.io/#x15.4.4.19
if (!Array.prototype.map) {

    Array.prototype.map = function(callback/*, thisArg*/) {

        var T, A, k;

        if (this == null) {
            throw new TypeError('this is null or not defined');
        }

        // 1. Let O be the result of calling ToObject passing the |this|
        //    value as the argument.
        var O = Object(this);

        // 2. Let lenValue be the result of calling the Get internal
        //    method of O with the argument "length".
        // 3. Let len be ToUint32(lenValue).
        var len = O.length >>> 0;

        // 4. If IsCallable(callback) is false, throw a TypeError exception.
        // See: http://es5.github.com/#x9.11
        if (typeof callback !== 'function') {
            throw new TypeError(callback + ' is not a function');
        }

        // 5. If thisArg was supplied, let T be thisArg; else let T be undefined.
        if (arguments.length > 1) {
            T = arguments[1];
        }

        // 6. Let A be a new array created as if by the expression new Array(len)
        //    where Array is the standard built-in constructor with that name and
        //    len is the value of len.
        A = new Array(len);

        // 7. Let k be 0
        k = 0;

        // 8. Repeat, while k < len
        while (k < len) {

            var kValue, mappedValue;

            // a. Let Pk be ToString(k).
            //   This is implicit for LHS operands of the in operator
            // b. Let kPresent be the result of calling the HasProperty internal
            //    method of O with argument Pk.
            //   This step can be combined with c
            // c. If kPresent is true, then
            if (k in O) {

                // i. Let kValue be the result of calling the Get internal
                //    method of O with argument Pk.
                kValue = O[k];

                // ii. Let mappedValue be the result of calling the Call internal
                //     method of callback with T as the this value and argument
                //     list containing kValue, k, and O.
                mappedValue = callback.call(T, kValue, k, O);

                // iii. Call the DefineOwnProperty internal method of A with arguments
                // Pk, Property Descriptor
                // { Value: mappedValue,
                //   Writable: true,
                //   Enumerable: true,
                //   Configurable: true },
                // and false.

                // In browsers that support Object.defineProperty, use the following:
                // Object.defineProperty(A, k, {
                //   value: mappedValue,
                //   writable: true,
                //   enumerable: true,
                //   configurable: true
                // });

                // For best browser support, use the following:
                A[k] = mappedValue;
            }
            // d. Increase k by 1.
            k++;
        }

        // 9. return A
        return A;
    };
}
// Production steps of ECMA-262, Edition 5, 15.4.4.21
// Reference: http://es5.github.io/#x15.4.4.21
// https://tc39.github.io/ecma262/#sec-array.prototype.reduce
if (!Array.prototype.reduce) {
    Object.defineProperty(Array.prototype, 'reduce', {
        value: function(callback /*, initialValue*/) {
            if (this === null) {
                throw new TypeError( 'Array.prototype.reduce ' +
                    'called on null or undefined' );
            }
            if (typeof callback !== 'function') {
                throw new TypeError( callback +
                    ' is not a function');
            }

            // 1. Let O be ? ToObject(this value).
            var o = Object(this);

            // 2. Let len be ? ToLength(? Get(O, "length")).
            var len = o.length >>> 0;

            // Steps 3, 4, 5, 6, 7
            var k = 0;
            var value;

            if (arguments.length >= 2) {
                value = arguments[1];
            } else {
                while (k < len && !(k in o)) {
                    k++;
                }

                // 3. If len is 0 and initialValue is not present,
                //    throw a TypeError exception.
                if (k >= len) {
                    throw new TypeError( 'Reduce of empty array ' +
                        'with no initial value' );
                }
                value = o[k++];
            }

            // 8. Repeat, while k < len
            while (k < len) {
                // a. Let Pk be ! ToString(k).
                // b. Let kPresent be ? HasProperty(O, Pk).
                // c. If kPresent is true, then
                //    i.  Let kValue be ? Get(O, Pk).
                //    ii. Let accumulator be ? Call(
                //          callbackfn, undefined,
                //          « accumulator, kValue, k, O »).
                if (k in o) {
                    value = callback(value, o[k], k, o);
                }

                // d. Increase k by 1.
                k++;
            }

            // 9. Return accumulator.
            return value;
        }
    });
}
// Production steps of ECMA-262, Edition 5, 15.4.4.22
// Reference: http://es5.github.io/#x15.4.4.22
if ('function' !== typeof Array.prototype.reduceRight) {
    Array.prototype.reduceRight = function(callback /*, initialValue*/) {
        'use strict';
        if (null === this || 'undefined' === typeof this) {
            throw new TypeError('Array.prototype.reduce called on null or undefined');
        }
        if ('function' !== typeof callback) {
            throw new TypeError(callback + ' is not a function');
        }
        var t = Object(this), len = t.length >>> 0, k = len - 1, value;
        if (arguments.length >= 2) {
            value = arguments[1];
        } else {
            while (k >= 0 && !(k in t)) {
                k--;
            }
            if (k < 0) {
                throw new TypeError('Reduce of empty array with no initial value');
            }
            value = t[k--];
        }
        for (; k >= 0; k--) {
            if (k in t) {
                value = callback(value, t[k], k, t);
            }
        }
        return value;
    };
}
if (!Array.prototype.includes) {
    Array.prototype.includes = function(e) {
        var k;
        for (k = 0; k < this.length; k++) {
            if (e === this[k]) return true;
        }
        return false;
    }
}
if (!Object.entries) {
    Object.entries = function( obj ){
        var ownProps = Object.keys( obj ),
            i = ownProps.length,
            resArray = new Array(i); // preallocate the Array
        while (i--)
          resArray[i] = [ownProps[i], obj[ownProps[i]]];

        return resArray;
    };
}
// haskell-inspired array functions
Array.prototype.head = function() {
    return this[0]
}
Array.prototype.last = function() {
    return this[this.length - 1]
}
Array.prototype.tail = function() {
    return this.slice(1)
}
Array.prototype.init = function() {
    return this.slice(0, this.length - 1)
}
Array.prototype.shuffle = function() {
    let counter = this.length;

    // While there are elements in the array
    while (counter > 0) {
        // Pick a random index
        let index = Math.floor(Math.random() * counter);

        // Decrease counter by 1
        counter--;

        // And swap the last element with it
        let temp = this[counter];
        this[counter] = this[index];
        this[index] = temp;
    }

    return this;
}
Array.prototype.sum = function(selector) {
    return this.reduce((acc,val) => acc + ((selector === undefined) ? val : selector(val)), 0)
}
Array.prototype.max = function(selector) {
    return this.reduce((acc,val) => (((selector === undefined) ? val : selector(val)) > acc) ? ((selector === undefined) ? val : selector(val)) : acc, 0)
}

///////////////////////////////////////////////////////////////////////////////////////////////////
//  NOTE TO PROGRAMMERS: this JS_INIT script does not, and must not be invoked with strict mode  //
///////////////////////////////////////////////////////////////////////////////////////////////////

// disabling and re-installing JS/Nashorn functions
// alse see: https://wiki.openjdk.java.net/display/Nashorn/Nashorn+extensions
load = undefined;
loadWithNewGlobal = undefined;
exit = undefined;
quit = undefined;
printErr = undefined;
readbuffer = undefined;
readline = undefined;
/*var eval = function(s) { // this impl is flawed; it does not return any, and cannot alter Global which may not you actually want
    return Function('"use strict";return(function(){'+s+'}())')();
}*/
//
function javaArrayToJs(jarr) {
    if (!jarr.toString.startsWith("[")) return jarr;
    var arr = [];
    for (var k = 0; k < jarr.length; k++) {
        arr.push(jarr[k]);
    }
    return arr;
}
// standard print functions
function print(s) {
    sys.print(s);
}
function println(s) {
    if (s === undefined)
        sys.print("\n");
    else
        sys.println(s);
}
function printerr(s) {
    print("\x1B[31m"+s+"\x1B[m");
}
function printerrln(s) {
    println("\x1B[31m"+s+"\x1B[m");
}
function read() {
    return sys.read();
}
String.prototype.trimNull = function() {
    let cnt = this.length - 1
    while (cnt >= 0) {
        if (this.charCodeAt(cnt) != 0) break;
        cnt -= 1;
    }
    return this.slice(0, cnt + 1);
}
// ncurses-like terminal control
var con = {};
con.KEY_HOME = 199;
con.KEY_UP = 200;
con.KEY_PAGE_UP = 201;
con.KEY_LEFT = 203;
con.KEY_RIGHT = 205;
con.KEY_END = 207;
con.KEY_DOWN = 208
con.KEY_PAGE_DOWN = 209;
con.KEY_INSERT = 210;
con.KEY_DELETE = 211;
con.KEY_BACKSPACE = 8;
con.KEY_TAB = 9;
con.KEY_RETURN = 10;
con.getch = function() {
    return sys.readKey();
};
con.move = function(y, x) {
    print("\x1B["+(y|0)+";"+(x|0)+"H");
};
con.addch = function(c) {
    graphics.putSymbol(c|0);
};
con.mvaddch = function(y, x, c) {
    con.move(y, x); con.addch(c);
};
con.getmaxyx = function() {
    return graphics.getTermDimension();
};
con.getyx = function() {
    return graphics.getCursorYX();
};
con.curs_up = function() {
    let c = graphics.getCursorYX();
    con.move(c[0]-1,c[1]);
};
con.curs_down = function() {
    let c = graphics.getCursorYX();
    con.move(c[0]+1,c[1]);
};
con.curs_left = function() {
    let c = graphics.getCursorYX();
    con.move(c[0],c[1]-1);
};
con.curs_right = function() {
    let c = graphics.getCursorYX();
    con.move(c[0],c[1]+1);
};
con.hitterminate = function() { // ^C
    sys.poke(-40, 1);
    return (sys.peek(-41) == 31 && (sys.peek(-42) == 129 || sys.peek(-42) == 130));
};
con.hiteof = function() { // ^D
    sys.poke(-40, 1);
    return (sys.peek(-41) == 32 && (sys.peek(-42) == 129 || sys.peek(-42) == 130));
};
con.resetkeybuf = function() {
    sys.poke(-40, 0);
    sys.poke(-41, 0); sys.poke(-42, 0); sys.poke(-43, 0); sys.poke(-44, 0);
    sys.poke(-45, 0); sys.poke(-46, 0); sys.poke(-47, 0); sys.poke(-48, 0);
};
con.video_reverse = function() {
    print("\x1B[7m");
};
con.color_fore = function(n) { // 0..7; -1 for transparent
    if (n < 0)
        print("\x1B[38;5;255m");
    else
        print("\x1B["+(((n|0) % 8)+30)+"m");
};
con.color_back = function(n) { // 0..7; -1 for transparent
    if (n < 0)
        print("\x1B[48;5;255m");
    else
        print("\x1B["+(((n|0) % 8)+40)+"m");
};
con.color_pair = function(fore, back) { // 0..255
    print("\x1B[38;5;"+fore+"m");
    print("\x1B[48;5;"+back+"m");
};
con.clear = function() {
    print("\x1B[2J");
};
// @params arg 0 to hide, nonzero to show
con.curs_set = function(arg) {
    print("\x1B[?25"+(((arg|0) == 0) ? "l" : "h"));
};
con.reset_graphics = function() {
    print("\x1B[m");
};
// returns current key-down status
con.poll_keys = function() {
    sys.poke(-40, 1);
    return [-41,-42,-43,-44,-45,-46,-47,-48].map(it => sys.peek(it));
};
Object.freeze(con);
// system management function
var system = {};
system.maxmem = function() {
    return sys.peek(-65) | (sys.peek(-66) << 8) | (sys.peek(-67) << 16) | (sys.peek(-68) << 24);
};
Object.freeze(system);
// some utilities functions

if (Graal !== undefined && !Graal.isGraalRuntime()) {
    serial.printerr("GraalVM compiler is not running, expect low performance");
}
