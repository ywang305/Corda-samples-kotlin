package net.corda.samples.obligation.contract


import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.obligation.states.IOUState
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.utils.sumCash
/**
 * This is where you'll add the contract code which defines how the [IOUState] behaves. Look at the unit tests in
 * [IOUContractTests] for instructions on how to complete the [IOUContract] class.
 */
class IOUContract : Contract {
    companion object {
        @JvmStatic
        val IOU_CONTRACT_ID = "net.corda.samples.obligation.contract.IOUContract"
    }

    /**
     * Add any commands required for this contract as classes within this interface.
     * It is useful to encapsulate your commands inside an interface, so you can use the [requireSingleCommand]
     * function to check for a number of commands which implement this interface.
     */
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
    }

    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Issue -> verifyIssuance(tx, command)
            else -> throw RuntimeException("Unrecognised command in this contract.")
        }
    }

    private fun verifyIssuance(tx: LedgerTransaction, commandData: CommandWithParties<Commands>) {
        require(tx.inputStates.isEmpty()) { "No inputs should be consumed when issuing an IOU." }
        require(tx.outputStates.size == 1) { "Only one output state should be created when issuing an IOU." }
        val iouState = tx.outputsOfType<IOUState>().single()
        require(iouState.amount > Amount.zero(iouState.amount.token)) {
            "A newly issued IOU must have a positive amount."
        }
        require(iouState.lender != iouState.borrower) { "The lender and borrower cannot have the same identity." }
        val signers = commandData.signers
        require(signers.toSet() == iouState.participants.map { it.owningKey }.toSet()) {
            "Both lender and borrower together only may sign IOU issue transaction."
        }
    }
}