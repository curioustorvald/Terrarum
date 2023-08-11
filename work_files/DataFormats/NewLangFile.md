#### Formal Grammar

```
Text = Tag , Text | PlainText , Text ;

Tag = "{" , Code , { TagArgs } , "}" ;
TagArgs = " " , Text ;
Code = Number | "G" | "P" | "KC" | "SYM" | "NORMAL" | "EMPH" | "VERB" | "RED" | Code , "." , Subcode ;
    (* KC: keycap. e.g. "{KC control_key_interact}" | "{KC e}" *)
    (* G: grammatical gender *)
    (* P: singualr/plural *)
    (* EMPH: emphasize noun. e.g. "Treasure for the {EMPH}paraglider{NORMAL}. A fair exchange, I believe." *)
    (* Number: n-th substitution target. e.g. "Treasure for the {EMPH}{0}{NORMAL}. A fair exchange, I believe." *)
    (* NORMAL: unset emphases. *)
    (* VERB: emphasize verb. e.g. "Press {KC use} to pay {VERB} respects" *)
    (* RED: red text. e.g. "Saving, {RED}do not turn off the power{NORMAL}..." *)

Subcode = Number | Case | Count ;
Case = "NOM" | "ACC" | "GEN" | "DAT" | ... ;
Count = "SG" | "DU" | "PL" ;

Number = { "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" } ;

PlainText = ? regular string but does not contain { c U r L y } brackets ? ;
```

#### Code Example

```
function ShowMsg(string: String, vararg args: String) { ... } // pre-defined

val m = "Give {0} {P.0 1} to {2.ACC}"
ShowMsg(m, 42, "ITEM_COAL", conversationTarget.actorValue.name)

val m2 = "{0}{G.0 을 를} 찾을 수 없습니다"
ShowMsg(m2, something)
```
