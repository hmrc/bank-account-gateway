### Bank Account Insights

This API enables your application to get an opinion of the riskiness of a sortcode and bank account combination. 

Given a sort code and account number, the response will provide:

* Risk score from 0 (no risk) to 100 (high risk)
* Reason providing an indication of why the risk score has been allocated
* Correlation Id - so you can reference the transaction in any feedback

### Bank Account Verification

This API enables your application to verify that a personal or business bank account exists.
Given an account number, sortcode and account name the response will indicate whether:

* The sortcode exists in the EISCD database of known sortcodes.
* The accountNumber/sortCode combination is valid according to static checks (the modcheck).
* If the above succeed, whether the account name provided matches the name held for the given account.
