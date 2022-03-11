Multiple JSON files can exist under this directory.

### Ingredient Querying Language

An ingredient can refer one exact item or items that matches the conditions.

To specify single exact item:

    ID IS item@basegame:1

To specify using tags:

    TAG HASALLOF a_tag,b_tag,c_tag OR TAG IS s_tag
    TAG IS one_tag
    TAG HASALLOF a_tag,b_tag

The query can have one or more terms. The terms are always either one of:

    TERM  OPERATOR  TERM
    TAG_LITERAL

Since "TAG" is a valid term to operate against, said word can frequently appear for complex queries.

#### List of Operators

- IS : Exactly this
- HASALLOF : Has all of these
- HASSOMEOF : Has one or more of these
- HASNONEOF : Has none of these
- ISNOT : Exactly not this
- AND : Both left and right terms are truthy
- OR : One or more hands are truthy
- , : Creates an array containing two or more words separated by this operator

#### List of Predefined Operands

- TAG : Tags assigned to the blocks or items
- ID : Item ID

