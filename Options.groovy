@Grab(group='commons-cli', module='commons-cli', version='1.2')
import org.apache.commons.cli.HelpFormatter

@Singleton
class Options {

    private static final USAGE   = 'calc.groovy [options]'
    private static final HEADER  = ''
    private static final FOOTER  = ''
    private static final OPTIONS = [
        asteroid:  [args:    1,
                    argName: 'type',
                    type:    String,
                    defVal:  null,
                    desc:    'Fuzzy match asteroid type.'],

        quantity:  [args:    1,
                    argName: 'amount',
                    type:    BigDecimal,
                    defVal:  null,
                    desc:    'Amount of ore.'],

        units:     [args:    0,
                    argName: '',
                    type:    Boolean,
                    defVal:  true,
                    desc:    'Quantity given in units. Default.'],

        volume:    [args:    0,
                    argName: '',
                    type:    Boolean,
                    defVal:  false,
                    desc:   'Quantity given in volume.'],

        refining:  [args:    1,
                    argName: 'level',
                    type:    Integer,
                    defVal:  5,
                    desc:    'Refining skill level (1 though 5). Default 5.'],

        effciency: [args:    1,
                    argName: 'level',
                    type:    Integer,
                    defVal:  5,
                    desc:   'Refining Effciency skill level (1 though 5). Default 5.'],

        specific:  [args:    1,
                    argName: 'level',
                    type:    Integer,
                    defVal:  5,
                    desc: 'Ore Specific skill level (1 though 5). Default 5.'],

        station:   [args:    1,
                    argName: 'level',
                    type:    BigDecimal,
                    defVal:  '50.0'.toBigDecimal(),
                    desc:    'Station Equipment level (37.5 or 50.0). Default 50.0.'],

        system:    [args:    1,
                    argName: 'system',
                    type:    String,
                    defVal:  null,
                    desc:    'Fuzzy match System refined ore will be sold in.'],

        forceDB:   [args:    0,
                    argName: '',
                    type:    Boolean,
                    defVal:  false,
                    desc:    'Force the local database to be reloaded from the EVE toolkit ported MySQL database.'],

        printDB:   [args:    0,
                    argName: '',
                    type:    Boolean,
                    defVal:  false,
                    desc:    'Print the values in the local database rather than calculate.']
    ]

    private options
    private cli

    Options() {
        cli = new CliBuilder(usage: USAGE, header: HEADER, footer: FOOTER)
        cli.formatter = new HelpFormatter()
        cli.width = 119

        cli.h longOpt: 'help', 'Prints this help message.'

        OPTIONS.each {k, v ->
            Map usage = [longOpt: k]
            if (v.args) usage.args = v.args
            if (v.argName) usage.argName = v.argName

            def desc = v.desc ?: "Default Value: ${v.defVal}, ".padRight(20) +
                                 "Type: ${v.type.getSimpleName()}"

            if (v.short) cli."${v.short}"(usage, desc)
            else cli._(usage, desc)
        }
    }

    void initialize(String[] args) {
        options = cli.parse(args)
        if (!isValid()) cli.usage()
    }

    Object getAt(String name, Object defaultValue = null) {
        getProperty(name) ?: (defaultValue ?: OPTIONS.get(name).defVal)
    }

    Object getProperty(String name) {
        if (name == 'cli') return cli

        Object prop = options.getProperty(name)

        if (prop && OPTIONS.containsKey(name)) {
            return prop.asType(OPTIONS.get(name).type)
        }

        return prop
    }

    Boolean isValid() {
        return options != null && !options.help
    }

}
