import groovy.transform.Immutable
import groovy.util.slurpersupport.GPathResult

@Singleton
class EveCentral {

    private final XmlSlurper slurper = new XmlSlurper()

    static final BASE_URL = 'http://api.eve-central.com/api'
    static final MARKETSTAT = "$BASE_URL/marketstat"
    static final QUICKLOOK ="$BASE_URL/quicklook"

    BigDecimal getPrice(EveType type, EveRegion region = null) {
        String query = "$QUICKLOOK?typeid=${type.typeID}"
        query = region ? query + "&regionlimit=${region.regionID}" : query

        String response = query.toURL().getText()
        //println response
        GPathResult xml = slurper.parseText(response)

        xml.quicklook.buy_orders.order.price.collect {
            new BigDecimal(it.text())
        }.max()
    }
}
