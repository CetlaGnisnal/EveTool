import groovy.transform.Immutable

@Immutable
class EveRegion {
    Integer regionID
    String regionName

    @Override
    String toString() {
        regionName
    }
}
