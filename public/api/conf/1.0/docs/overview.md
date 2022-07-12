This API enables your application to verify that a personal or business bank account exists.
Given an account number, sortcode and account name the response will indicate whether:

* The sortcode exists in the EISCD database of known sortcodes.
* The accountNumber/sortCode combination is valid according to static checks (the modcheck).
* If the above succeed, whether the account name provided matches the name held for the given account.
