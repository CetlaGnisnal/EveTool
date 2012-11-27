class Calc {

    static void main(String[] args) {
        bootstrap()

        Options options = Options.getInstance()
        options.initialize(args)

        if (!options.isValid()) return

        Database database = Database.getInstance()
        database.createLocalDatabase()

        if(options.printDB) {
            database.printDB()
            return
        }

        new Calc().calculate()
    }

    private static void bootstrap() {
        Object.metaClass.getOptions {-> Options.getInstance() }
        Object.metaClass.getDatabase {-> Database.getInstance() }
    }

    void calculate() {

    }
}
