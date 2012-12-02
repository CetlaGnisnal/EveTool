@Grab(group='commons-cli', module='commons-cli', version='1.2')
import org.apache.commons.cli.HelpFormatter

@Singleton
class Options {

    private static final USAGE   = 'Calc [options]'
    private static final HEADER  = ''
    private static final FOOTER  = ''
    private static final OPTIONS = [
        material:  [args:    1,
                    argName: 'type',
                    short:   'm',
                    type:    String,
                    defVal:  null,
                    desc:    'The type of material to be used in calculations. This will be fuzzy matched, for example \'veld\' would match to \'Veldspar\''],

        quantity:  [args:    1,
                    argName: 'amount',
                    short:   'q',
                    type:    BigDecimal,
                    defVal:  null,
                    desc:    'The amount of material to be used in calculations. This by default is given in units but can be given in volume by using the \'--volume\' option.'],

        units:     [args:    0,
                    argName: '',
                    type:    Boolean,
                    defVal:  true,
                    desc:    'Specifies that the quantity is given in \'Units\'. This is the default behavior and using this option changes nothing.'],

        volume:    [args:    0,
                    argName: '',
                    type:    Boolean,
                    defVal:  false,
                    desc:   'Specifies that the quantity is given in a volume of cubic meters.'],

        refining:  [args:    1,
                    argName: 'level',
                    type:    Integer,
                    defVal:  5,
                    desc:    '\'Refining\' skill level (0 though 5). Default 5.'],

        efficiency:[args:    1,
                    argName: 'level',
                    type:    Integer,
                    defVal:  3,
                    desc:   '\'Refining Efficiency\' skill level (0 though 5). Default 3.'],

        specific:  [args:    1,
                    argName: 'level',
                    type:    Integer,
                    defVal:  2,
                    desc: '\'Ore Specific\' skill level, for example \'Veldspar Refining\'. (0 though 5). Default 2.'],

        equipment: [args:    1,
                    argName: 'level',
                    type:    BigDecimal,
                    defVal:  '50.0'.toBigDecimal(),
                    desc:    '\'Station Equipment\' level (37.5 or 50.0). This can be found at the top right of the refining window. Default 50.0.'],

        station:   [args:    1,
                    argName: 'standing',
                    short:   's',
                    type:    BigDecimal,
                    defVal:  '0'.toBigDecimal(),
                    desc:    '\'Station Standing\' affects the amount of refined materials that a station keeps. Default 0.'],

        corpTax:   [args:    1,
                    argName: 'rate',
                    type:    BigDecimal,
                    defVal:  '11.0'.toBigDecimal(),
                    desc:     'The \'Corporate Tax Rate\' is use in calculating the net sale since a corporation takes a percentage of all sales. Default 11.0%'],

        region:    [args:    1,
                    argName: 'name',
                    short:   'r',
                    type:    String,
                    defVal:  null,
                    desc:    'The region to check Eve Central for prices. This helps make estimation more accurate. The name is fuzzy matched so \'Vervendo\' would find \'Verge Vendor\'. If no region is given all of space will be searched.'],

        forceDB:   [args:    0,
                    argName: '',
                    type:    Boolean,
                    defVal:  false,
                    desc:    'Force the local database to be reloaded from the CPP Static Dump database. The URL to the database can be given by the \'--cppDump\' option.'],

        cppDump:   [args:    1,
                    argName: 'url',
                    type:    String,
                    defVal:  'jdbc:mysql://localhost/eve?user=root',
                    desc:    'Connection string pointing to the CPP Static Dump database. Currently MySQL or H2 databases are supported. This is only needed when (re)building the local database. Default: jdbc:mysql://localhost/eve?user=root'],

        printDB:   [args:    0,
                    argName: '',
                    type:    Boolean,
                    defVal:  false,
                    desc:    'Print the values in the local database rather than calculate. TODO move this to a command.'],

        verbose:   [args:    0,
                    argName: '',
                    short:   'v',
                    type:    Boolean,
                    defVal:  false,
                    desc:    'Print lots of details about the calculation.'],
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
