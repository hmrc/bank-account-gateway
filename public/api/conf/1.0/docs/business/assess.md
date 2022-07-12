# Bank Account Reputation Microservice - Business Assess Endpoint

## Overview

### What and how?
This endpoint checks the _likely_ correctness of a given business bank account and it's _likely_ connection to the given business.

The following checks are performed:
1. The format and content of the sort code and account number, via the [BACS standard modulus check](https://www.vocalink.com/tools/modulus-checking/). (This is an internal check, so highly resilient).
1. The sort code's existence in a local copy of a UK sort code database ([EISCD](https://www.bacs.co.uk/Resources/Pages/EISCD.aspx)). (This is an internal check, so highly resilient).
1. The account detail's existence within a third party business bank account dataset. (This calls off to an external service, so may fail).
1. If the third-party _does_ locate the account, then the similarity of the business details it holds, compared with those provided. (This calls off to an external service, so may fail).

**Note**: If calls to the third party service fail, the results of earlier checks are still returned. 

### How to use the results
This service has two benefits:
* It can increase the likelihood of correct bank details being captured, by having the user check and correct details that _may_ be incorrect, in the event of an error.
* It can indicate if the transaction might be fraudulent (e.g. the business details held against the matched bank details don't match the business details provided).

It's important to note that the sort code database and third party service have *incomplete* coverage. Because of this, for user-facing services, negative results from related checks do *not* mean the provided bank/business details are *necessarily* incorrect, but rather that the user should check and correct provided details before retrying. It may be appropriate to allow the user journey to proceed along a happy-path, even if the sort code database check and third party service checks remain negative. 

For more details see [Interpreting the response](#interpreting-the-response).

## Usage
### URL and Method
URL: `/verify/business`

Method: `POST`

### Request message body
A JSON message equivalent to the following Scala `Input` case class and its associates:

```scala
case class Account(
                    sortCode: String, // The bank sort code, 6 characters long (whitespace and/or dashes should be removed)
                    accountNumber: String // The bank account number, 8 characters long
                  )


case class Address(
                    lines: List[String], // One to four lines; cumulative length must be between 1 and 140 characters.
                    town: Option[String], // Must be between 1 and 35 characters long
                    postcode: Option[String]
                  )


case class Business(
                    companyName: String, // Must be between 1 and 70 characters long
                    address: Option[Address]
                  )

case class Input(
                  account: Account,
                  business: Option[Business]
                )
```

### Headers
* `User-Agent` - required; any string identifying the calling client, often the name of the microservice, supplied when using http-verbs.
* `X-Tracking-Id` - optional; any string identifying the current _journey_ that will be inserted into audit trails.
* `X-Request-ID` - required; a value representing the request, supplied by default when using http-verbs
* `X-Session-ID` - required (for web); a value representing the session, supplied by default when using http-verbs
* `True-Client-IP` - required; the IP address of the external user, supplied by default when using http-verbs
* `True-Client-Port` - required; the port of the external user, supplied by default when using http-verbs

### Response status codes
* **200 OK** given a valid request and the reputation assessment was successfully obtained.
* Others as per [General Error Codes](../../../../../../docs/README.md).

### Response body
* `Content-Type: application/json`

The response is a JSON message equivalent to the following Scala:

```
case class Response(
    accountNumberIsWellFormatted: String,
    sortCodeIsPresentOnEISCD: String,
    sortCodeBankName: Option[String],
    nonStandardAccountDetailsRequiredForBacs: String,
    accountExists: String,
    nameMatches: String,
    accountName: Option[String],
    sortCodeSupportsDirectDebit: String,
    sortCodeSupportsDirectCredit: String,
    iban: Option[String]
)
```

## Interpreting the response
### `accountNumberIsWellFormatted`
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | The sort code is known and the sort code and account number are correctly formatted and their contents pass our implementation of the [BACS standard modulus check](https://www.vocalink.com/tools/modulus-checking/). They may still not resolve to an addressable (for electronic transfer) bank account though. | In the absence of other checks, the user journey should proceed to the next stage. |
| `no` | The sort code is known but the account number and sort code combination did not pass out implementation of the [BACS standard modulus check](https://www.vocalink.com/tools/modulus-checking/). | The user should be given the opportunity to try again, but not proceed if the error continues. |
| `indeterminate` | The sort code is unknown and would not resolve to an addressable bank account, therefore it cannot be mod checked. | This should be treated as a neutral response; defer to other factors such as accountExists to make a decision. |

### `sortCodeIsPresentOnEISCD` 
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | The sort code exists in the [EISCD](https://www.bacs.co.uk/Resources/Pages/EISCD.aspx) database. | In the absence of other checks, the user journey should proceed to the next stage. |
| `no` | The sort code does not exist in the EISCD. This could be because: <ul><li>The sort code was mistyped or is completely wrong.</li><li>The sort code is valid but is missing from the database.</li></ul> | The user should be given the opportunity to try again, but not proceed if the error continues. |
| `error` | There was an error checking the EISCD. | Either retry (system or user initiated), or proceed to the next check/stage of user journey. |

### `sortCodeBankName` 
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| _A bank name_ | If the sort code was found in the EISCD, then this field will contain the Bank Name held against it. | If present, this could be replayed back to the user, as part of a confirmation page. |

### `nonStandardAccountDetailsRequiredForBacs` 
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | Indicates that a BACS transaction for this account will require additional information (e.g. a Building Society roll number). This applies to a minority of accounts. | The additional information can not be verified in any way, so when `yes` is returned for this field, the calling service should ensure the user provides a non-empty additional information parameter as part of the BACS transaction. |
| `no` | See above. | No action required. |
| `inapplicable` | This value will be returned when `accountNumberIsWellFormatted` is `no`. | No action required. |

### `accountExists` 
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | The sort code and account number relate to a bank account stored in the third party service. | In the absence of other checks, the user journey should proceed to the next stage. |
| `no` | The third party service asserted that the account number was not valid for the given sort code. | The user should be given the opportunity to try again, but not proceed if the error continues. |
| `inapplicable` | The sort code and/or account number failed initial validation, and so no further checks were made. | The user should be given the opportunity to try again, but not proceed if the error continues. |
| `indeterminate` | The sort code and account number were not found in the third party service. This could be because:<ul><li>One of them was mistyped, or completely wrong.</li><li>The bank account and sort code are valid but are missing from the service.</li></ul> | It’s not possible to distinguish between these two reasons. The user should be given the opportunity to try again, and then proceed along the happy path should the error continue. |
| `error` | There was an error checking the third party service. | Either retry (system or user initiated), or proceed to the next check/stage of user journey. |

### `nameMatches` 
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | After normalisation, the provided business name was found to match one stored by the third party service. | In the absence of other checks, the user journey should proceed to the next stage. |
| `partial` | After normalisation, the provided business name was found to be a close match to the one stored by the third party service E.g 'ACME inc' vs 'ACME incorporated'. | This should almost always be treated as a `yes`, but in certain edge cases you may want to give the user the opportunity to try again. When this occurs, the name held by the third party is returned in the `accountName` field for your service to perform further checks if required. |
| `no` | After normalisation, no match was found for the provided business name in the third party service. | This could be because it was mistyped, or is completely wrong. The user should be given the opportunity to try again. The calling service needs to decide if it's appropriate to proceed along the happy path should the error continue. For example, if the post code also failed to match, then it _might_ be more appropriate to not proceed. |
| `inapplicable` | No company name was entered, or the sort code and/or account number failed initial validation and no further checks were made | This response can be ignored if the user was not required to enter a company name, otherwise they should be given the opportunity to try again but not proceed if the error continues. |
| `indeterminate` | The sort code and account number did not resolve to an account stored in the third party service. | See row for `accountExists`: `no` and `indeterminate`. |
| `error` | There was an error checking the third party service. | Either retry (system or user initiated), or proceed to the next check/stage of user journey. |

### `accountName`
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| E.g. `ACME Inc` | In the case of a partial name match, this will be the name on the account as reported by the third party. | Depending on your use case, you can compare this to the input provided by the user to provide extra certainty about your decision to proceed with the user journey. Most services will not need to use this information. |
| `<not present>` | The result for nameMatches was not a partial match |

### `sortCodeSupportsDirectDebit`
| Response | Interpretation | Handling within user journey |
|:---------|:---------------|:-----------------------------|
| `yes` | The provided sortcode supports direct debit payments and setup. | |
| `no` | The provided sortcode does not support direct debit payments and/or setup. | |
| `error` | There was an error checking the eiscd. | |

### `sortCodeSupportsDirectCredit`
| Response | Interpretation | Handling within user journey |
|:---------|:---------------|:-----------------------------|
| `yes` | The provided sortcode supports direct credit payments. | |
| `no` | The provided sortcode does not support direct credit payments. | |
| `error` | There was an error checking the third party service. | |

### `iban`
| Response        | Interpretation                                                                                                                        | Handling within user journey |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------| ----------- |
| A valid IBAN    | Both modcheck and EISCD validation were successful and a IBAN for the UK bank account is generated                                    |  |
| `<not present>` | An IBAN could not be generated: EISCD validation passed but `bicBankCode` wasn't defined, modcheck failed, or EISCD validation failed |  |

## General Error Conditions
See [README](../../../../../../docs/README.md)
