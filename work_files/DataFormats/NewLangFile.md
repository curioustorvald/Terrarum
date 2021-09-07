#### Formal Grammar

````
Text = Tag , Text | PlainText , Text ;

Tag = "{" , Code , { TagArgs } , "}" ;
TagArgs = " " , Text ;
Code = Number | "G" | "P" | "KC" | "NORMAL" | "EMPH" | "VERB" | "RED" | Code , "." , Subcode ;
    (* KC: keycap. e.g. "{KC config_keyinteract}" | "{KC e}" *)
    (* G:  *)
    (* EMPH: emphasize noun. e.g. "Treasure for the {EMPH}paraglider{NORMAL}. A fair exchange, I believe." *)
    (* Number: n-th substitution target. e.g. "Treasure for the {EMPH}{0}{NORMAL}. A fair exchange, I believe." *)
    (* NORMAL: unset emphases. *)
    (* VERB: emphasize verb. e.g. "Press {KC use} to pay {VERB} respects" *)
    (* RED: red text. e.g. "Saving, {RED}do not turn off the power{NORMAL}..." *)

Subcode = Case | Count ;
Case = "NOM" | "ACC" | "GEN" | "DAT" | ... ;
Count = "SG" | "DU" | "PL" ;

Number = { "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" } ;

PlainText = ? regular string but does not contain { C u R l Y } brackets ? ;
````

#### Code Example

```
function ShowMsg(string: String, vararg args: String) { ... } // pre-defined

val m = "Give {0} {P 1 0} to {2.ACC}"
ShowMsg(m, 42, "GAME_ITEM_COAL", conversationTarget.actorValue.name)
```
