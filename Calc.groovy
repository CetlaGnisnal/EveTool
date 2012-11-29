@Grab(group='org.apache.commons', module='commons-lang3', version='3.1')

import org.apache.commons.lang3.StringUtils

class Calc {

    EveType asteroid
    EveRegion region
    Integer quantity
    Integer refinePortions
    Integer refineQuantity
    Integer refiningSkill
    Integer effciencySkill
    Integer specificSkill
    Integer stationLevel
    BigDecimal effectiveYield

    Map<EveType, Integer> oreYields

    FuzzyMatch<EveType> typeMatcher
    FuzzyMatch<EveRegion> regionMatcher

    static void main(String[] args) {
        bootstrap()

        ScriptConsole console = ScriptConsole.getInstance()
        console.addHeader 'Cetla\'s Ore Calc Utility'

        Options options = Options.getInstance()
        options.initialize(args)

        if (!options.isValid()) return

        Database database = Database.getInstance()
        database.createLocalDatabase()

        if(options.printDB) {
            console.updateStatus 'Printing Database'
            database.printDB()
            return
        }

        new Calc().calculate()
    }

    Calc() {
        console.updateStatus 'Loading Values from Database'

        typeMatcher = new FuzzyMatch(corpus: database.fetchAllTypes(), matchOn: { it.typeName })
        regionMatcher = new FuzzyMatch(corpus: database.fetchAllRegions(), matchOn: { it.regionName })
    }

    private static void bootstrap() {
        Object.metaClass.getConsole {-> ScriptConsole.getInstance() }
        Object.metaClass.getOptions {-> Options.getInstance() }
        Object.metaClass.getDatabase {-> Database.getInstance() }
        Object.metaClass.getEveCentral {-> EveCentral.getInstance() }
    }

    void calculate() {
        setAsteroid()
        setQuantity()
        setSkills()
        setEffectiveYield()
        setOreYields()
        setRegion()

        Map<EveType, Map<String, Integer>> yields = oreYields.collectEntries {asteroid, base ->
            Integer yield = effectiveYield * base
            BigDecimal standing = new BigDecimal('1.2')
            BigDecimal taxRate = 5 - (new BigDecimal('0.75') * standing)
            taxRate = taxRate < 0 ? BigDecimal.ZERO : taxRate / 100
            taxRate = taxRate.setScale(4)
            Integer tax = yield * taxRate
            Integer taxedYield = yield - tax
            Integer total = taxedYield * refinePortions

            Map details = [base: base, yield: yield, taxRate: taxRate, tax: tax, taxedYield: taxedYield, total: total]
            [(asteroid): details]
        }

        def xlarge = 15
        def large = 12
        def med = 8
        def small = 6
        def ixlarge = xlarge - 2
        def ilarge = large - 2
        def imed = med - 2
        def ismall = small - 2


        console.addLine()
        console.addStatus "Yield Table for ${asteroid.typeName}", 'YELLOW'
        console.addStatus '┏' + pad('Ore', xlarge, '━') + '┯' +
                                pad('Base', small, '━') + '┯' +
                                pad('Yield', small, '━') + '┯' +
                                pad('Tax', small, '━') + '┯' +
                                pad('Net', small, '━') + '┯' +
                                pad('Total', large, '━') + '┯┯' +
                                pad('Sell', med, '━') + '┯' +
                                pad('Gross', xlarge, '━') + '┯' +
                                pad('Tax', large, '━') + '┯' +
                                pad('Corp', large, '━') + '┳┳' +
                                pad('Net', xlarge, '━') + '┓'

        def line = {
            console.addStatus '┠' + '─' * xlarge + '┼' +
                                    '─' * small + '┼' +
                                    '─' * small + '┼' +
                                    '─' * small + '┼' +
                                    '─' * small + '┼' +
                                    '─' * large + '┼┼' +
                                    '─' * med + '┼' +
                                    '─' * xlarge + '┼' +
                                    '─' * large + '┼' +
                                    '─' * large + '╂╂' +
                                    '─' * xlarge + '┨'
        }

        yields.each {ore, details->
            String base = '┃ ' + ore.typeName.padRight(ixlarge) + ' ┆ ' +
                                 details.base.toString().padLeft(ismall) + '   ' +
                                 details.yield.toString().padLeft(ismall) + '   ' +
                                 details.tax.toString().padLeft(ismall) + '   ' +
                                 details.taxedYield.toString().padLeft(ismall) + '   ' +
                                 details.total.toString().padLeft(ilarge) + ' ╎╎ '

            console.updateStatus base + 'Looking up price...' + ''.padLeft(30) + '┃┃' + '┃'.padLeft(xlarge + 3)

            def price = eveCentral.getPrice(ore, region)

            def gross = price * details.total
            def tax = (gross * 0.015).setScale(2, BigDecimal.ROUND_DOWN)
            def taxedGross = (gross - tax).setScale(2, BigDecimal.ROUND_DOWN)
            def corp = (taxedGross * 0.11).setScale(2, BigDecimal.ROUND_DOWN)
            def net = (taxedGross - corp).setScale(2, BigDecimal.ROUND_DOWN)

            details.net = net

            console.addStatus base + price.toString().padLeft(imed) + '   ' +
                                     gross.toString().padLeft(ixlarge) + '   ' +
                                     tax.toString().padLeft(ilarge) + '   ' +
                                     corp.toString().padLeft(ilarge) + ' ┃┃ ' +
                                     net.toString().padLeft(ixlarge) + ' ┃'

            line()
        }

        line()

        def netTotal = yields.collect {o, d-> d.net }.sum()

        console.addStatus '┃ ' + 'Refine Total'.padRight(ixlarge) + ' ┆ ' +
                             ''.padLeft(ismall) + '   ' +
                             ''.padLeft(ismall) + '   ' +
                             ''.padLeft(ismall) + '   ' +
                             ''.padLeft(ismall) + '   ' +
                             ''.padLeft(ilarge) + ' ╎╎ ' +
                             ''.padLeft(imed) + '   ' +
                             ''.padLeft(ixlarge) + '   ' +
                             ''.padLeft(ilarge) + '   ' +
                             ''.padLeft(ilarge) + ' ┃┃ ' +
                             netTotal.toString().padLeft(ixlarge) + ' ┃'

        line()

        String base = '┃ ' + asteroid.typeName.padRight(ixlarge) + ' ┆ ' +
                             ''.padLeft(ismall) + '   ' +
                             ''.padLeft(ismall) + '   ' +
                             ''.padLeft(ismall) + '   ' +
                             ''.padLeft(ismall) + '   ' +
                             quantity.toString().padLeft(ilarge) + ' ╎╎ '

        console.updateStatus base + 'Looking up price...' + ''.padLeft(30) + '┃┃' + '┃'.padLeft(xlarge + 3)

        def price = eveCentral.getPrice(asteroid, region)

        def gross = price * quantity
        def tax = (gross * 0.015).setScale(2, BigDecimal.ROUND_DOWN)
        def taxedGross = (gross - tax).setScale(2, BigDecimal.ROUND_DOWN)
        def corp = (taxedGross * 0.11).setScale(2, BigDecimal.ROUND_DOWN)
        def net = (taxedGross - corp).setScale(2, BigDecimal.ROUND_DOWN)

        console.addStatus base + price.toString().padLeft(imed) + '   ' +
                                     gross.toString().padLeft(ixlarge) + '   ' +
                                     tax.toString().padLeft(ilarge) + '   ' +
                                     corp.toString().padLeft(ilarge) + ' ┃┃ ' +
                                     net.toString().padLeft(ixlarge) + ' ┃'

        console.addStatus '┗' + ('━' * xlarge) + '┷' +
                                ('━' * small) + '┷' +
                                ('━' * small) + '┷' +
                                ('━' * small) + '┷' +
                                ('━' * small) + '┷' +
                                ('━' * large) + '┷┷' +
                                ('━' * med) + '┷' +
                                ('━' * xlarge) + '┷' +
                                ('━' * large) + '┷' +
                                ('━' * large) + '┻┻' +
                                ('━' * xlarge) + '┛'

        console.addLine()
        if(netTotal > net) {
            console.addStatus "Refining is more profitable by ${(100 * (1 - (net / netTotal))).setScale(2, BigDecimal.ROUND_DOWN)}%.", 'GREEN'
        } else {
            console.addStatus "Selling is more profitable by ${(100 * (1 - (netTotal / net))).setScale(2, BigDecimal.ROUND_DOWN)}%.", 'RED'
        }
        console.addLine()
    }

    private String pad(String s, int n, String p) {
        // StringUtils is croaking on the pad characters
        def nn = n - s.length() - 1
        nn = nn < 0 ? 0 : nn
        (p * 1) + s + (p * nn)
    }

    void setOreYields() {
        oreYields = database.fetchRefineYield(asteroid)

        console.addStatus 'Base Refining Yields'
        def max = oreYields.keySet().typeName*.length().max()
        oreYields.each {a, y ->
            console.addStatus "\t${a.typeName.padLeft(max)}: ${y}"
        }
    }

    void setEffectiveYield() {
        // TODO Round to 1?
        def a = (stationLevel / 100) + (0.375 *
                (1 + (refiningSkill * 0.02)) *
                (1 + (effciencySkill * 0.04)) *
                (1 + (specificSkill * 0.05)))
        effectiveYield = a < 1 ? a : new BigDecimal('1.00')

        console.addStatus 'Effective Refining Yield'
        console.addStatus "\t Personal: 0.375 + ❨1+❪${refiningSkill}×.02❫❩ × ❨1+❪${effciencySkill}×.04❫❩ × ❨1+❪${specificSkill}×.05❫❩"
        console.addStatus "\t  Station: ❨${stationLevel} ÷ 100❩"
        console.addStatus "\tEffective: ${effectiveYield}"
    }

    void setAsteroid() {
        String optAsteroid = options['material']
        Collection optAsteroidMatch = []
        if (optAsteroid) {
            optAsteroidMatch = typeMatcher.findAll(optAsteroid)
            if (optAsteroidMatch.size() == 1) {
                asteroid = optAsteroidMatch.first()
            }
        }

        if(!asteroid) {
            console.updateStatus 'Set the asteroid type.'
            asteroid = withConfirm { askFuzzy('Asteroid Type?', typeMatcher, optAsteroidMatch) }
        }

        console.addStatus "Asteroid Type: ${asteroid}"
        console.addStatus "\tVolume: ${asteroid.volume}m3"
        console.addStatus "\tRefine: ${asteroid.portionSize}units"
    }

    void setRegion() {
        String optRegion = options['region']
        Collection optRegionMatch = []
        if(optRegion) {
            optRegionMatch = regionMatcher.findAll(optRegion)
            if (optRegionMatch.size() == 1) {
                region = optRegionMatch.first()
            }
        }

        if(!region) {
            console.updateStatus 'Set the region for price lookup.'
            region = withConfirm { askFuzzy('Region?', regionMatcher, optRegionMatch) }
        }

        console.addStatus 'Price Quote Region'
        console.addStatus "\tRegion: ${region.regionName}"
    }

    void setQuantity() {
        BigDecimal optQuant = options['quantity']
        if (optQuant) {
            if (options['units'] && !options['volume']) {
                quantity = optQuant
            } else {
                quantity = optQuant / asteroid.volume
            }
        }

        if(!quantity) {
            console.updateStatus 'Set asteroid quantity.'
            def units = console.userInput('Unit of Measure?', ['u','v']) == 'u'
            quantity = withConfirm { console.userInput(units ? 'Units?' : 'Volume?') }.toInteger()
            if (!units) quantity / asteroid.volume
        }

        // TODO using Integer math for rounding. Does eve always round down?
        refinePortions = (Integer) (quantity / asteroid.portionSize)
        refineQuantity = refinePortions * asteroid.portionSize

        console.addStatus "Asteroid Quantity"
        console.addStatus "\t   Units: ${quantity}"
        console.addStatus "\t  Volume: ${quantity * asteroid.volume}m3"
        console.addStatus "\t  Refine: ${refineQuantity}"
        console.addStatus "\tPortions: ${refinePortions}"
        console.addStatus "\t  Remain: ${quantity - refineQuantity}"
    }

    void setSkills() {
        refiningSkill = options['refining']
        effciencySkill = options['efficiency']
        specificSkill = options['specific']
        stationLevel = options['equipment']
    }

    private <V> V withConfirm(Closure<V> question) {
        V value = null
        def confirmed = false
        while(!confirmed) {
            value = question()
            def yn = console.userInput("Use ${value}?", ['y', 'n'])
            confirmed = yn == 'y'
        }
        return value
    }

    private <V> V askFuzzy(String question, FuzzyMatch<V> fm, Collection<V> previous = []) {
        V match = null
        while(!match) {
            def userInput = console.userInput(question, previous.collect(fm.matchOn), false)
            def matches = fm.findAll(userInput)

            if (matches.size() == 1) {
                match = matches.first()
            } else {
                previous = matches
            }
        }
        return match
    }
}

