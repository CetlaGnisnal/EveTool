import groovy.transform.Immutable

@Immutable
class EveType {
    Integer typeID
    String typeName
    Double volume
    Integer portionSize
    String category

    @Override
    String toString() { typeName }
}
