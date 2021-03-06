/*
 * Copyright ish group pty ltd. All rights reserved. http://www.ish.com.au No copying or use of this code is allowed without permission in writing from ish.
 */
package ish.oncourse.server.cayenne

import ish.CayenneIshTestCase
import ish.common.types.AccountTransactionType
import ish.common.types.CreditCardType
import ish.common.types.PaymentSource
import ish.common.types.PaymentStatus
import ish.common.types.VoucherPaymentStatus
import ish.math.Money
import ish.oncourse.common.BankingType
import ish.oncourse.entity.services.SetPaymentMethod
import ish.oncourse.server.ICayenneService
import ish.util.AccountUtil
import ish.util.PaymentMethodUtil
import static junit.framework.TestCase.assertEquals
import org.apache.cayenne.ObjectContext
import org.apache.cayenne.query.ObjectSelect
import org.apache.cayenne.query.SelectById
import org.dbunit.dataset.ReplacementDataSet
import org.dbunit.dataset.xml.FlatXmlDataSet
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder
import org.junit.Before
import org.junit.Test
import static org.testng.Assert.assertFalse
import static org.testng.Assert.assertNotNull
import static org.testng.Assert.assertNull
import static org.testng.Assert.assertTrue

import java.time.LocalDate

class TransactionsCreationServerTest extends CayenneIshTestCase {

	private ICayenneService cayenneService

    @Before
    void setup() throws Exception {
		wipeTables()
        this.cayenneService = injector.getInstance(ICayenneService.class)

        InputStream st = DoublePrefetchTest.class.getClassLoader().getResourceAsStream("ish/oncourse/server/cayenne/TransactionsCreationServertTestDataSet.xml")
        FlatXmlDataSet dataSet = new FlatXmlDataSetBuilder().build(st)
        ReplacementDataSet rDataSet = new ReplacementDataSet(dataSet)

        executeDatabaseOperation(rDataSet)

        super.setup()
    }
	
	@Test
    void testAutoBank() {
		ObjectContext context = cayenneService.getNewContext()

        Invoice invoice = SelectById.query(Invoice.class, 1L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.IN_TRANSACTION)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getRealTimeCreditCardPaymentMethod(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setCreditCardName("test name")
        paymentIn.setCreditCardNumber("test number")
        paymentIn.setCreditCardExpiry("01/01")
        paymentIn.setCreditCardType(CreditCardType.VISA)
        paymentIn.setAmount(invoice.getAmountOwing())
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(invoice.getAmountOwing())
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        context.commitChanges()
        context.invalidateObjects(line)


        List<AccountTransaction> select = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.and(AccountTransaction.FOREIGN_RECORD_ID.eq(line.getId()))
				.select(context)
        assertTrue(select.isEmpty())
        assertNull(paymentIn.getBanking())

        paymentIn.setStatus(PaymentStatus.SUCCESS)

        context.commitChanges()

        assertNotNull(paymentIn.getBanking())

        List<AccountTransaction> transactions = ObjectSelect.query(AccountTransaction.class).select(context)
        assertEquals(2,transactions.size())

        AccountTransaction deposit = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getAccountIn()))
				.and(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.selectOne(context)

        assertNotNull(deposit)
        assertEquals(new Money("100.00"), deposit.getAmount())


        AccountTransaction undeposit = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getUndepositedFundsAccount()))
				.and(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.selectOne(context)

        assertNull(undeposit)

        AccountTransaction debtor = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(line.getInvoice().getDebtorsAccount()))
				.and(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.selectOne(context)

        assertNotNull(debtor)
        assertEquals(new Money("-100.00"), debtor.getAmount())
    }


	@Test
    void testChangeBanking() {
		ObjectContext context = cayenneService.getNewContext()

        Invoice invoice = SelectById.query(Invoice.class, 1L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.SUCCESS)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getRealTimeCreditCardPaymentMethod(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setCreditCardName("test name")
        paymentIn.setCreditCardNumber("test number")
        paymentIn.setCreditCardExpiry("01/01")
        paymentIn.setCreditCardType(CreditCardType.VISA)
        paymentIn.setAmount(invoice.getAmountOwing())
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(invoice.getAmountOwing())
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        //bank automatically and check transactions
		context.commitChanges()

        List<AccountTransaction> select = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.and(AccountTransaction.FOREIGN_RECORD_ID.eq(line.getId()))
				.select(context)
        assertFalse(select.isEmpty())
        assertNotNull(paymentIn.getBanking())

        assertTrue(ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getUndepositedFundsAccount())).select(context).isEmpty())

        AccountTransaction deposit = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getAccountIn()))
				.and(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.selectOne(context)

        assertNotNull(deposit)
        assertEquals(new Money("100.00"), deposit.getAmount())

        AccountTransaction debtor = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(line.getInvoice().getDebtorsAccount()))
				.and(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.selectOne(context)

        assertNotNull(debtor)
        assertEquals(new Money("-100.00"), debtor.getAmount())

        //unbank and check transactions
		paymentIn.setBanking(null)
        context.commitChanges()

        AccountTransaction undeposit = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getUndepositedFundsAccount())).selectOne(context)
        assertNotNull(undeposit)
        assertEquals(new Money("100.00"), undeposit.getAmount())

        List<AccountTransaction> deposits = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getAccountIn())).select(context)
        assertEquals(2, deposits.size())
        Money depositsSumm = deposits.collect{it.amount}.inject(Money.ZERO) {a, b -> a.add(b)}
        assertEquals(Money.ZERO, depositsSumm)


        //bank again to another banking and check transactions (final state)
		LocalDate newSettlementDate = LocalDate.of(2016,6,12)
        Banking banking = context.newObject(Banking.class)
        banking.setSettlementDate(newSettlementDate)
        banking.setType(BankingType.AUTO_MCVISA)

        paymentIn.setBanking(banking)
        context.commitChanges()

        AccountTransaction undepositNew = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getUndepositedFundsAccount()))
				.and(AccountTransaction.TRANSACTION_DATE.eq(newSettlementDate))
				.selectOne(context)

        assertNotNull(undepositNew)
        assertEquals(new Money("-100.00"), undepositNew.getAmount())

        AccountTransaction depositNew = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getAccountIn()))
				.and(AccountTransaction.TRANSACTION_DATE.eq(newSettlementDate))
				.selectOne(context)

        assertNotNull(depositNew)
        assertEquals(new Money("100.00"), depositNew.getAmount())

        List<AccountTransaction>  allUndeposit = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getUndepositedFundsAccount()))
				.select(context)
        List<AccountTransaction> allDeposit = ObjectSelect.query(AccountTransaction.class).where(AccountTransaction.ACCOUNT.eq(paymentIn.getAccountIn()))
				.select(context)

        assertEquals(2, allUndeposit.size())

        assertEquals(3, allDeposit.size())

        assertEquals(Money.ZERO, allUndeposit.collect{it.amount}.inject(Money.ZERO) {a, b -> a.add(b)})
        assertEquals(new Money("100.00"), allDeposit.collect{it.amount}.inject(Money.ZERO) {a, b -> a.add(b)})

    }

	@Test
    void testZeroPaymentInLineUseCase() {
		ObjectContext context = cayenneService.getNewContext()

        Invoice invoice = SelectById.query(Invoice.class, 1L).selectOne(context)
        Invoice invoice3 = SelectById.query(Invoice.class, 3L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.IN_TRANSACTION)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getRealTimeCreditCardPaymentMethod(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setCreditCardName("test name")
        paymentIn.setCreditCardNumber("test number")
        paymentIn.setCreditCardExpiry("01/01")
        paymentIn.setCreditCardType(CreditCardType.VISA)
        paymentIn.setAmount(invoice.getAmountOwing())
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(invoice.getAmountOwing())
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        PaymentInLine line2 = context.newObject(PaymentInLine.class)
        line2.setAmount(Money.ZERO)
        line2.setPayment(paymentIn)
        line2.setInvoice(invoice3)
        line2.setAccountOut(invoice3.getDebtorsAccount())

        context.commitChanges()
        context.invalidateObjects(line, line2)

        List<AccountTransaction> select1 = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.and(AccountTransaction.FOREIGN_RECORD_ID.eq(line.getId()))
				.select(context)
        List<AccountTransaction> select2 = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.and(AccountTransaction.FOREIGN_RECORD_ID.eq(line2.getId()))
				.select(context)
        assertTrue(select1.isEmpty())
        assertTrue(select2.isEmpty())
        assertNull(paymentIn.getBanking())

        paymentIn.setStatus(PaymentStatus.SUCCESS)

        context.commitChanges()

        select1 = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.and(AccountTransaction.FOREIGN_RECORD_ID.eq(line.getId()))
				.select(context)
        select2 = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.and(AccountTransaction.FOREIGN_RECORD_ID.eq(line2.getId()))
				.select(context)
        assertFalse(select1.isEmpty())
        assertTrue(select2.isEmpty())
        assertNotNull(paymentIn.getBanking())

        paymentIn.setBanking(null)

        context.commitChanges()

        select1 = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.and(AccountTransaction.FOREIGN_RECORD_ID.eq(line.getId()))
				.select(context)
        select2 = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.TABLE_NAME.eq(AccountTransactionType.PAYMENT_IN_LINE))
				.and(AccountTransaction.FOREIGN_RECORD_ID.eq(line2.getId()))
				.select(context)
        assertFalse(select1.isEmpty())
        assertTrue(select2.isEmpty())
    }


    //VoucherProduct Purchase price: $10
    //Redemption : 3 enrolments
    //1)Class cost for redemption : $100
    //2)Class cost for redemption : $30
	@Test
    void testPaymentTransactionWithVoucherEnrolment1() {
		ObjectContext context = cayenneService.getNewContext()

        Account assetAccount =  SelectById.query(Account.class, 1000L).selectOne(context)
        Account liabilityAccount = AccountUtil.getDefaultVoucherLiabilityAccount(context, Account.class)
        Account expenseAccount = AccountUtil.getDefaultVoucherExpenseAccount(context, Account.class)

        Invoice invoice = SelectById.query(Invoice.class, 7L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.SUCCESS)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setAmount(Money.ZERO)
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(Money.ZERO)
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        PaymentIn paymentInVoucher = context.newObject(PaymentIn.class)
        paymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        paymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), paymentInVoucher).set()
        paymentInVoucher.setAmount(invoice.getAmountOwing())
        paymentInVoucher.setPayer(invoice.getContact())
        paymentInVoucher.setPaymentDate(LocalDate.now())
        paymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine lineVoucher = context.newObject(PaymentInLine.class)
        lineVoucher.setAmount(invoice.getAmountOwing())
        lineVoucher.setPayment(paymentInVoucher)
        lineVoucher.setInvoice(invoice)
        lineVoucher.setAccountOut(invoice.getDebtorsAccount())

        Voucher voucher = SelectById.query(Voucher.class, 1L).selectOne(context)
        voucher.setRedeemedCourseCount(voucher.getRedeemedCourseCount() + 1)

        VoucherPaymentIn voucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        voucherPaymentIn.setPaymentIn(paymentInVoucher)
        voucherPaymentIn.setVoucher(voucher)
        voucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)
        voucherPaymentIn.setInvoiceLine(invoice.getInvoiceLines().get(0))


        context.commitChanges()

        List<AccountTransaction> transactions = ObjectSelect.query(AccountTransaction.class).select(context)
        assertEquals(4, transactions.size())

        List<AccountTransaction> assetTransactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(transactions)
        assertEquals(1, assetTransactions.size())
        assertEquals(new Money("-100.00"), assetTransactions.get(0).getAmount())

        List<AccountTransaction> liabilityTransactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(transactions)
        assertEquals(1, liabilityTransactions.size())
        assertEquals(new Money("-10.00"), liabilityTransactions.get(0).getAmount())

        List<AccountTransaction> expenseTransactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(transactions)
        assertEquals(2, expenseTransactions.size())

        AccountTransaction asset_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(1) : expenseTransactions.get(0)
        AccountTransaction liability_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(0) : expenseTransactions.get(1)

        assertEquals(new Money("100.00"), asset_expense.getAmount())
        assertEquals(new Money("-10.00"), liability_expense.getAmount())


        //redeem next time this voucher

		Invoice secondInvoice = SelectById.query(Invoice.class, 8L).selectOne(context)

        PaymentIn secondPaymentIn = context.newObject(PaymentIn.class)
        secondPaymentIn.setStatus(PaymentStatus.SUCCESS)
        secondPaymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), secondPaymentIn).set()
        secondPaymentIn.setAmount(Money.ZERO)
        secondPaymentIn.setPayer(secondInvoice.getContact())
        secondPaymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine secondLine = context.newObject(PaymentInLine.class)
        secondLine.setAmount(Money.ZERO)
        secondLine.setPayment(secondPaymentIn)
        secondLine.setInvoice(secondInvoice)
        secondLine.setAccountOut(secondInvoice.getDebtorsAccount())

        PaymentIn secondPaymentInVoucher = context.newObject(PaymentIn.class)
        secondPaymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        secondPaymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), secondPaymentInVoucher).set()
        secondPaymentInVoucher.setAmount(secondInvoice.getAmountOwing())
        secondPaymentInVoucher.setPayer(secondInvoice.getContact())
        secondPaymentInVoucher.setPaymentDate(LocalDate.now())
        secondPaymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine secondLineVoucher = context.newObject(PaymentInLine.class)
        secondLineVoucher.setAmount(secondInvoice.getAmountOwing())
        secondLineVoucher.setPayment(secondPaymentInVoucher)
        secondLineVoucher.setInvoice(secondInvoice)
        secondLineVoucher.setAccountOut(secondInvoice.getDebtorsAccount())

        voucher.setRedeemedCourseCount(voucher.getRedeemedCourseCount() + 1)

        VoucherPaymentIn secondVoucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        secondVoucherPaymentIn.setPaymentIn(secondPaymentInVoucher)
        secondVoucherPaymentIn.setVoucher(voucher)
        secondVoucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)
        secondVoucherPaymentIn.setInvoiceLine(secondInvoice.getInvoiceLines().get(0))

        context.commitChanges()

        List<AccountTransaction> secondTransactions = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.FOREIGN_RECORD_ID.eq(secondLineVoucher.getId()))
				.select(context)
        assertEquals(2, secondTransactions.size())

        List<AccountTransaction> asset2Transactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(secondTransactions)
        assertEquals(1, asset2Transactions.size())
        assertEquals(new Money("-30.00"), asset2Transactions.get(0).getAmount())

        List<AccountTransaction> expense2Transactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(secondTransactions)
        assertEquals(1, expense2Transactions.size())
        assertEquals(new Money("30.00"), expense2Transactions.get(0).getAmount())
    }

    //VoucherProduct Purchase price: $120
    //Redemption : 3 enrolments
    //1)Class cost for redemption : $100
    //2)Class cost for redemption : $30
    @Test
    void testPaymentTransactionWithVoucherEnrolment2() {
        ObjectContext context = cayenneService.getNewContext()

        Account assetAccount =  SelectById.query(Account.class, 1000L).selectOne(context)
        Account liabilityAccount = AccountUtil.getDefaultVoucherLiabilityAccount(context, Account.class)
        Account expenseAccount = AccountUtil.getDefaultVoucherExpenseAccount(context, Account.class)

        Invoice invoice = SelectById.query(Invoice.class, 7L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.SUCCESS)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setAmount(Money.ZERO)
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(Money.ZERO)
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        PaymentIn paymentInVoucher = context.newObject(PaymentIn.class)
        paymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        paymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), paymentInVoucher).set()
        paymentInVoucher.setAmount(invoice.getAmountOwing())
        paymentInVoucher.setPayer(invoice.getContact())
        paymentInVoucher.setPaymentDate(LocalDate.now())
        paymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine lineVoucher = context.newObject(PaymentInLine.class)
        lineVoucher.setAmount(invoice.getAmountOwing())
        lineVoucher.setPayment(paymentInVoucher)
        lineVoucher.setInvoice(invoice)
        lineVoucher.setAccountOut(invoice.getDebtorsAccount())

        Voucher voucher = SelectById.query(Voucher.class, 4L).selectOne(context)
        voucher.setRedeemedCourseCount(voucher.getRedeemedCourseCount() + 1)

        VoucherPaymentIn voucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        voucherPaymentIn.setPaymentIn(paymentInVoucher)
        voucherPaymentIn.setVoucher(voucher)
        voucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)
        voucherPaymentIn.setInvoiceLine(invoice.getInvoiceLines().get(0))


        context.commitChanges()

        List<AccountTransaction> transactions = ObjectSelect.query(AccountTransaction.class).select(context)
        assertEquals(4, transactions.size())

        List<AccountTransaction> assetTransactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(transactions)
        assertEquals(1, assetTransactions.size())
        assertEquals(new Money("-100.00"), assetTransactions.get(0).getAmount())

        List<AccountTransaction> liabilityTransactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(transactions)
        assertEquals(1, liabilityTransactions.size())
        assertEquals(new Money("-100.00"), liabilityTransactions.get(0).getAmount())

        List<AccountTransaction> expenseTransactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(transactions)
        assertEquals(2, expenseTransactions.size())

        AccountTransaction asset_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(1) : expenseTransactions.get(0)
        AccountTransaction liability_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(0) : expenseTransactions.get(1)

        assertEquals(new Money("100.00"), asset_expense.getAmount())
        assertEquals(new Money("-100.00"), liability_expense.getAmount())


        //redeem next time this voucher

        Invoice secondInvoice = SelectById.query(Invoice.class, 8L).selectOne(context)

        PaymentIn secondPaymentIn = context.newObject(PaymentIn.class)
        secondPaymentIn.setStatus(PaymentStatus.SUCCESS)
        secondPaymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), secondPaymentIn).set()
        secondPaymentIn.setAmount(Money.ZERO)
        secondPaymentIn.setPayer(secondInvoice.getContact())
        secondPaymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine secondLine = context.newObject(PaymentInLine.class)
        secondLine.setAmount(Money.ZERO)
        secondLine.setPayment(secondPaymentIn)
        secondLine.setInvoice(secondInvoice)
        secondLine.setAccountOut(secondInvoice.getDebtorsAccount())

        PaymentIn secondPaymentInVoucher = context.newObject(PaymentIn.class)
        secondPaymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        secondPaymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), secondPaymentInVoucher).set()
        secondPaymentInVoucher.setAmount(secondInvoice.getAmountOwing())
        secondPaymentInVoucher.setPayer(secondInvoice.getContact())
        secondPaymentInVoucher.setPaymentDate(LocalDate.now())
        secondPaymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine secondLineVoucher = context.newObject(PaymentInLine.class)
        secondLineVoucher.setAmount(secondInvoice.getAmountOwing())
        secondLineVoucher.setPayment(secondPaymentInVoucher)
        secondLineVoucher.setInvoice(secondInvoice)
        secondLineVoucher.setAccountOut(secondInvoice.getDebtorsAccount())

        voucher.setRedeemedCourseCount(voucher.getRedeemedCourseCount() + 1)

        VoucherPaymentIn secondVoucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        secondVoucherPaymentIn.setPaymentIn(secondPaymentInVoucher)
        secondVoucherPaymentIn.setVoucher(voucher)
        secondVoucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)
        secondVoucherPaymentIn.setInvoiceLine(secondInvoice.getInvoiceLines().get(0))

        context.commitChanges()

        List<AccountTransaction> secondTransactions = ObjectSelect.query(AccountTransaction.class)
                .where(AccountTransaction.FOREIGN_RECORD_ID.eq(secondLineVoucher.getId()))
                .select(context)
        assertEquals(4, secondTransactions.size())

        List<AccountTransaction> asset2Transactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(secondTransactions)
        assertEquals(1, asset2Transactions.size())
        assertEquals(new Money("-30.00"), asset2Transactions.get(0).getAmount())

        List<AccountTransaction> liability2Transactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(secondTransactions)
        assertEquals(1, liability2Transactions.size())
        assertEquals(new Money("-20.00"), liability2Transactions.get(0).getAmount())

        List<AccountTransaction> expense2Transactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(secondTransactions)
        assertEquals(2, expense2Transactions.size())

        AccountTransaction asset_expense2 = expense2Transactions.get(0).getAmount().isNegative() ? expense2Transactions.get(1) : expense2Transactions.get(0)
        AccountTransaction liability_expense2 = expense2Transactions.get(0).getAmount().isNegative() ? expense2Transactions.get(0) : expense2Transactions.get(1)

        assertEquals(new Money("30.00"), asset_expense2.getAmount())
        assertEquals(new Money("-20.00"), liability_expense2.getAmount())
    }

    //VoucherProduct Purchase price: $120
    //Redemption : 1 enrolments
    //1)Class cost for redemption : $100
    @Test
    void testPaymentTransactionWithVoucherEnrolment3() {
        ObjectContext context = cayenneService.getNewContext()

        Account assetAccount =  SelectById.query(Account.class, 1000L).selectOne(context)
        Account liabilityAccount = AccountUtil.getDefaultVoucherLiabilityAccount(context, Account.class)
        Account expenseAccount = AccountUtil.getDefaultVoucherExpenseAccount(context, Account.class)

        Invoice invoice = SelectById.query(Invoice.class, 7L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.SUCCESS)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setAmount(Money.ZERO)
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(Money.ZERO)
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        PaymentIn paymentInVoucher = context.newObject(PaymentIn.class)
        paymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        paymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), paymentInVoucher).set()
        paymentInVoucher.setAmount(invoice.getAmountOwing())
        paymentInVoucher.setPayer(invoice.getContact())
        paymentInVoucher.setPaymentDate(LocalDate.now())
        paymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine lineVoucher = context.newObject(PaymentInLine.class)
        lineVoucher.setAmount(invoice.getAmountOwing())
        lineVoucher.setPayment(paymentInVoucher)
        lineVoucher.setInvoice(invoice)
        lineVoucher.setAccountOut(invoice.getDebtorsAccount())

        Voucher voucher = SelectById.query(Voucher.class, 6L).selectOne(context)
        voucher.setRedeemedCourseCount(voucher.getRedeemedCourseCount() + 1)

        VoucherPaymentIn voucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        voucherPaymentIn.setPaymentIn(paymentInVoucher)
        voucherPaymentIn.setVoucher(voucher)
        voucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)
        voucherPaymentIn.setInvoiceLine(invoice.getInvoiceLines().get(0))


        context.commitChanges()

        List<AccountTransaction> transactions = ObjectSelect.query(AccountTransaction.class).select(context)
        assertEquals(4, transactions.size())

        List<AccountTransaction> assetTransactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(transactions)
        assertEquals(1, assetTransactions.size())
        assertEquals(new Money("-100.00"), assetTransactions.get(0).getAmount())

        List<AccountTransaction> liabilityTransactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(transactions)
        assertEquals(1, liabilityTransactions.size())
        assertEquals(new Money("-120.00"), liabilityTransactions.get(0).getAmount())

        List<AccountTransaction> expenseTransactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(transactions)
        assertEquals(2, expenseTransactions.size())

        AccountTransaction asset_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(1) : expenseTransactions.get(0)
        AccountTransaction liability_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(0) : expenseTransactions.get(1)

        assertEquals(new Money("100.00"), asset_expense.getAmount())
        assertEquals(new Money("-120.00"), liability_expense.getAmount())
    }

    //VoucherProduct Purchase price: $30
    //Redemption : $150
    //1)Class cost for redemption : $100
    //2)Class cost for redemption : $30
    @Test
    void testPaymentTransactionWithVoucherValue1() {
		ObjectContext context = cayenneService.getNewContext()

        Account assetAccount =  SelectById.query(Account.class, 1000L).selectOne(context)
        Account liabilityAccount = AccountUtil.getDefaultVoucherLiabilityAccount(context, Account.class)
        Account expenseAccount = AccountUtil.getDefaultVoucherExpenseAccount(context, Account.class)

        Invoice invoice = SelectById.query(Invoice.class, 7L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.SUCCESS)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setAmount(Money.ZERO)
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(Money.ZERO)
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        PaymentIn paymentInVoucher = context.newObject(PaymentIn.class)
        paymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        paymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), paymentInVoucher).set()
        paymentInVoucher.setAmount(invoice.getAmountOwing())
        paymentInVoucher.setPayer(invoice.getContact())
        paymentInVoucher.setPaymentDate(LocalDate.now())
        paymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine lineVoucher = context.newObject(PaymentInLine.class)
        lineVoucher.setAmount(invoice.getAmountOwing())
        lineVoucher.setPayment(paymentInVoucher)
        lineVoucher.setInvoice(invoice)
        lineVoucher.setAccountOut(invoice.getDebtorsAccount())

        Voucher voucher = SelectById.query(Voucher.class, 2L).selectOne(context)
        Money balance = voucher.getRedemptionValue().subtract(invoice.getAmountOwing())
        voucher.setRedemptionValue(balance)

        VoucherPaymentIn voucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        voucherPaymentIn.setPaymentIn(paymentInVoucher)
        voucherPaymentIn.setVoucher(voucher)
        voucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)

        context.commitChanges()


        List<AccountTransaction> transactions = ObjectSelect.query(AccountTransaction.class).select(context)
        assertEquals(4, transactions.size())

        List<AccountTransaction> assetTransactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(transactions)
        assertEquals(1, assetTransactions.size())
        assertEquals(new Money("-100.00"), assetTransactions.get(0).getAmount())

        List<AccountTransaction> liabilityTransactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(transactions)
        assertEquals(1, liabilityTransactions.size())
        assertEquals(new Money("-30.00"), liabilityTransactions.get(0).getAmount())

        List<AccountTransaction> expenseTransactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(transactions)
        assertEquals(2, expenseTransactions.size())

        AccountTransaction asset_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(1) : expenseTransactions.get(0)
        AccountTransaction liability_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(0) : expenseTransactions.get(1)

        assertEquals(new Money("100.00"), asset_expense.getAmount())
        assertEquals(new Money("-30.00"), liability_expense.getAmount())

        //redeem next time this voucher

		Invoice secondInvoice = SelectById.query(Invoice.class, 8L).selectOne(context)

        PaymentIn secondPaymentIn = context.newObject(PaymentIn.class)
        secondPaymentIn.setStatus(PaymentStatus.SUCCESS)
        secondPaymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), secondPaymentIn).set()
        secondPaymentIn.setAmount(Money.ZERO)
        secondPaymentIn.setPayer(secondInvoice.getContact())
        secondPaymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine secondLine = context.newObject(PaymentInLine.class)
        secondLine.setAmount(Money.ZERO)
        secondLine.setPayment(secondPaymentIn)
        secondLine.setInvoice(secondInvoice)
        secondLine.setAccountOut(secondInvoice.getDebtorsAccount())

        PaymentIn secondPaymentInVoucher = context.newObject(PaymentIn.class)
        secondPaymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        secondPaymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), secondPaymentInVoucher).set()
        secondPaymentInVoucher.setAmount(secondInvoice.getAmountOwing())
        secondPaymentInVoucher.setPayer(secondInvoice.getContact())
        secondPaymentInVoucher.setPaymentDate(LocalDate.now())
        secondPaymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine secondLineVoucher = context.newObject(PaymentInLine.class)
        secondLineVoucher.setAmount(secondInvoice.getAmountOwing())
        secondLineVoucher.setPayment(secondPaymentInVoucher)
        secondLineVoucher.setInvoice(secondInvoice)
        secondLineVoucher.setAccountOut(secondInvoice.getDebtorsAccount())

        Voucher secondVoucher = SelectById.query(Voucher.class, 2L).selectOne(context)

        VoucherPaymentIn secondVoucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        secondVoucherPaymentIn.setPaymentIn(secondPaymentInVoucher)
        secondVoucherPaymentIn.setVoucher(secondVoucher)
        secondVoucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)
        secondVoucherPaymentIn.setInvoiceLine(secondInvoice.getInvoiceLines().get(0))

        context.commitChanges()

        List<AccountTransaction> secondTransactions = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.FOREIGN_RECORD_ID.eq(secondLineVoucher.getId()))
				.select(context)
        assertEquals(2, secondTransactions.size())

        List<AccountTransaction> asset2Transactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(secondTransactions)
        assertEquals(1, asset2Transactions.size())
        assertEquals(new Money("-30.00"), asset2Transactions.get(0).getAmount())

        List<AccountTransaction> expense2Transactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(secondTransactions)
        assertEquals(1, expense2Transactions.size())
        assertEquals(new Money("30.00"), expense2Transactions.get(0).getAmount())
    }

    //VoucherProduct Purchase price: $120
    //Redemption : $120
    //1)Class cost for redemption : $100
    //2)Class cost for redemption : $30
    @Test
    void testPaymentTransactionWithVoucherValue2() {
        ObjectContext context = cayenneService.getNewContext()

        Account assetAccount =  SelectById.query(Account.class, 1000L).selectOne(context)
        Account liabilityAccount = AccountUtil.getDefaultVoucherLiabilityAccount(context, Account.class)
        Account expenseAccount = AccountUtil.getDefaultVoucherExpenseAccount(context, Account.class)

        Invoice invoice = SelectById.query(Invoice.class, 7L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.SUCCESS)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setAmount(Money.ZERO)
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(Money.ZERO)
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        PaymentIn paymentInVoucher = context.newObject(PaymentIn.class)
        paymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        paymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), paymentInVoucher).set()
        paymentInVoucher.setAmount(invoice.getAmountOwing())
        paymentInVoucher.setPayer(invoice.getContact())
        paymentInVoucher.setPaymentDate(LocalDate.now())
        paymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine lineVoucher = context.newObject(PaymentInLine.class)
        lineVoucher.setAmount(invoice.getAmountOwing())
        lineVoucher.setPayment(paymentInVoucher)
        lineVoucher.setInvoice(invoice)
        lineVoucher.setAccountOut(invoice.getDebtorsAccount())

        Voucher voucher = SelectById.query(Voucher.class, 5L).selectOne(context)
        Money balance = voucher.getRedemptionValue().subtract(invoice.getAmountOwing())
        voucher.setRedemptionValue(balance)

        VoucherPaymentIn voucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        voucherPaymentIn.setPaymentIn(paymentInVoucher)
        voucherPaymentIn.setVoucher(voucher)
        voucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)

        context.commitChanges()


        List<AccountTransaction> transactions = ObjectSelect.query(AccountTransaction.class).select(context)
        assertEquals(4, transactions.size())

        List<AccountTransaction> assetTransactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(transactions)
        assertEquals(1, assetTransactions.size())
        assertEquals(new Money("-100.00"), assetTransactions.get(0).getAmount())

        List<AccountTransaction> liabilityTransactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(transactions)
        assertEquals(1, liabilityTransactions.size())
        assertEquals(new Money("-100.00"), liabilityTransactions.get(0).getAmount())

        List<AccountTransaction> expenseTransactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(transactions)
        assertEquals(2, expenseTransactions.size())

        AccountTransaction asset_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(1) : expenseTransactions.get(0)
        AccountTransaction liability_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(0) : expenseTransactions.get(1)

        assertEquals(new Money("100.00"), asset_expense.getAmount())
        assertEquals(new Money("-100.00"), liability_expense.getAmount())

        //redeem next time this voucher

        Invoice secondInvoice = SelectById.query(Invoice.class, 8L).selectOne(context)

        PaymentIn secondPaymentIn = context.newObject(PaymentIn.class)
        secondPaymentIn.setStatus(PaymentStatus.SUCCESS)
        secondPaymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), secondPaymentIn).set()
        secondPaymentIn.setAmount(Money.ZERO)
        secondPaymentIn.setPayer(secondInvoice.getContact())
        secondPaymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine secondLine = context.newObject(PaymentInLine.class)
        secondLine.setAmount(Money.ZERO)
        secondLine.setPayment(secondPaymentIn)
        secondLine.setInvoice(secondInvoice)
        secondLine.setAccountOut(secondInvoice.getDebtorsAccount())

        PaymentIn secondPaymentInVoucher = context.newObject(PaymentIn.class)
        secondPaymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        secondPaymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), secondPaymentInVoucher).set()
        secondPaymentInVoucher.setAmount(secondInvoice.getAmountOwing())
        secondPaymentInVoucher.setPayer(secondInvoice.getContact())
        secondPaymentInVoucher.setPaymentDate(LocalDate.now())
        secondPaymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine secondLineVoucher = context.newObject(PaymentInLine.class)
        secondLineVoucher.setAmount(secondInvoice.getAmountOwing())
        secondLineVoucher.setPayment(secondPaymentInVoucher)
        secondLineVoucher.setInvoice(secondInvoice)
        secondLineVoucher.setAccountOut(secondInvoice.getDebtorsAccount())

        VoucherPaymentIn secondVoucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        secondVoucherPaymentIn.setPaymentIn(secondPaymentInVoucher)
        secondVoucherPaymentIn.setVoucher(voucher)
        secondVoucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)
        secondVoucherPaymentIn.setInvoiceLine(secondInvoice.getInvoiceLines().get(0))

        context.commitChanges()

        List<AccountTransaction> secondTransactions = ObjectSelect.query(AccountTransaction.class)
                .where(AccountTransaction.FOREIGN_RECORD_ID.eq(secondLineVoucher.getId()))
                .select(context)
        assertEquals(4, secondTransactions.size())

        List<AccountTransaction> asset2Transactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(secondTransactions)
        assertEquals(1, asset2Transactions.size())
        assertEquals(new Money("-30.00"), asset2Transactions.get(0).getAmount())

        List<AccountTransaction> liability2Transactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(secondTransactions)
        assertEquals(1, liability2Transactions.size())
        assertEquals(new Money("-20.00"), liability2Transactions.get(0).getAmount())

        List<AccountTransaction> expense2Transactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(secondTransactions)
        assertEquals(2, expense2Transactions.size())

        AccountTransaction asset_expense2 = expense2Transactions.get(0).getAmount().isNegative() ? expense2Transactions.get(1) : expense2Transactions.get(0)
        AccountTransaction liability_expense2 = expense2Transactions.get(0).getAmount().isNegative() ? expense2Transactions.get(0) : expense2Transactions.get(1)

        assertEquals(new Money("30.00"), asset_expense2.getAmount())
        assertEquals(new Money("-20.00"), liability_expense2.getAmount())

    }

    //VoucherProduct Purchase price: $180
    //Redemption : $100
    //1)Class cost for redemption : $100
    @Test
    void testPaymentTransactionWithVoucherValue3() {
        ObjectContext context = cayenneService.getNewContext()

        Account assetAccount =  SelectById.query(Account.class, 1000L).selectOne(context)
        Account liabilityAccount = AccountUtil.getDefaultVoucherLiabilityAccount(context, Account.class)
        Account expenseAccount = AccountUtil.getDefaultVoucherExpenseAccount(context, Account.class)

        Invoice invoice = SelectById.query(Invoice.class, 7L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.SUCCESS)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setAmount(Money.ZERO)
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(Money.ZERO)
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        PaymentIn paymentInVoucher = context.newObject(PaymentIn.class)
        paymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        paymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), paymentInVoucher).set()
        paymentInVoucher.setAmount(invoice.getAmountOwing())
        paymentInVoucher.setPayer(invoice.getContact())
        paymentInVoucher.setPaymentDate(LocalDate.now())
        paymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine lineVoucher = context.newObject(PaymentInLine.class)
        lineVoucher.setAmount(invoice.getAmountOwing())
        lineVoucher.setPayment(paymentInVoucher)
        lineVoucher.setInvoice(invoice)
        lineVoucher.setAccountOut(invoice.getDebtorsAccount())

        Voucher voucher = SelectById.query(Voucher.class, 7L).selectOne(context)
        Money balance = voucher.getRedemptionValue().subtract(invoice.getAmountOwing())
        voucher.setRedemptionValue(balance)

        VoucherPaymentIn voucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        voucherPaymentIn.setPaymentIn(paymentInVoucher)
        voucherPaymentIn.setVoucher(voucher)
        voucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)

        context.commitChanges()


        List<AccountTransaction> transactions = ObjectSelect.query(AccountTransaction.class).select(context)
        assertEquals(4, transactions.size())

        List<AccountTransaction> assetTransactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(transactions)
        assertEquals(1, assetTransactions.size())
        assertEquals(new Money("-100.00"), assetTransactions.get(0).getAmount())

        List<AccountTransaction> liabilityTransactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(transactions)
        assertEquals(1, liabilityTransactions.size())
        assertEquals(new Money("-180.00"), liabilityTransactions.get(0).getAmount())

        List<AccountTransaction> expenseTransactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(transactions)
        assertEquals(2, expenseTransactions.size())

        AccountTransaction asset_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(1) : expenseTransactions.get(0)
        AccountTransaction liability_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(0) : expenseTransactions.get(1)

        assertEquals(new Money("100.00"), asset_expense.getAmount())
        assertEquals(new Money("-180.00"), liability_expense.getAmount())
    }

    //VoucherProduct Purchase price: $200
    //Redemption : $200
    //1)Class cost for redemption : $100
    //2)Class cost for redemption : $30
	@Test
    void testPaymentTransactionWithVoucherPurchaseValue() {
		ObjectContext context = cayenneService.getNewContext()

        Account assetAccount =  SelectById.query(Account.class, 1000L).selectOne(context)
        Account liabilityAccount = AccountUtil.getDefaultVoucherLiabilityAccount(context, Account.class)
        Account expenseAccount = AccountUtil.getDefaultVoucherExpenseAccount(context, Account.class)

        Invoice invoice = SelectById.query(Invoice.class, 7L).selectOne(context)

        PaymentIn paymentIn = context.newObject(PaymentIn.class)
        paymentIn.setStatus(PaymentStatus.SUCCESS)
        paymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), paymentIn).set()
        paymentIn.setAmount(Money.ZERO)
        paymentIn.setPayer(invoice.getContact())
        paymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine line = context.newObject(PaymentInLine.class)
        line.setAmount(Money.ZERO)
        line.setPayment(paymentIn)
        line.setInvoice(invoice)
        line.setAccountOut(invoice.getDebtorsAccount())

        PaymentIn paymentInVoucher = context.newObject(PaymentIn.class)
        paymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        paymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), paymentInVoucher).set()
        paymentInVoucher.setAmount(invoice.getAmountOwing())
        paymentInVoucher.setPayer(invoice.getContact())
        paymentInVoucher.setPaymentDate(LocalDate.now())
        paymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine lineVoucher = context.newObject(PaymentInLine.class)
        lineVoucher.setAmount(invoice.getAmountOwing())
        lineVoucher.setPayment(paymentInVoucher)
        lineVoucher.setInvoice(invoice)
        lineVoucher.setAccountOut(invoice.getDebtorsAccount())

        Voucher voucher = SelectById.query(Voucher.class, 3L).selectOne(context)
        Money balance = voucher.getRedemptionValue().subtract(invoice.getAmountOwing())
        voucher.setRedemptionValue(balance)

        VoucherPaymentIn voucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        voucherPaymentIn.setPaymentIn(paymentInVoucher)
        voucherPaymentIn.setVoucher(voucher)
        voucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)

        context.commitChanges()


        List<AccountTransaction> transactions = ObjectSelect.query(AccountTransaction.class).select(context)
        assertEquals(4, transactions.size())

        List<AccountTransaction> assetTransactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(transactions)
        assertEquals(1, assetTransactions.size())
        assertEquals(new Money("-100.00"), assetTransactions.get(0).getAmount())

        List<AccountTransaction> liabilityTransactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(transactions)
        assertEquals(1, liabilityTransactions.size())
        assertEquals(new Money("-100.00"), liabilityTransactions.get(0).getAmount())

        List<AccountTransaction> expenseTransactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(transactions)
        assertEquals(2, expenseTransactions.size())

        AccountTransaction asset_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(1) : expenseTransactions.get(0)
        AccountTransaction liability_expense = expenseTransactions.get(0).getAmount().isNegative() ? expenseTransactions.get(0) : expenseTransactions.get(1)

        assertEquals(new Money("100.00"), asset_expense.getAmount())
        assertEquals(new Money("-100.00"), liability_expense.getAmount())


        //redeem next time this voucher

		Invoice secondInvoice = SelectById.query(Invoice.class, 8L).selectOne(context)

        PaymentIn secondPaymentIn = context.newObject(PaymentIn.class)
        secondPaymentIn.setStatus(PaymentStatus.SUCCESS)
        secondPaymentIn.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getINTERNALPaymentMethods(context, PaymentMethod.class), secondPaymentIn).set()
        secondPaymentIn.setAmount(Money.ZERO)
        secondPaymentIn.setPayer(secondInvoice.getContact())
        secondPaymentIn.setPaymentDate(LocalDate.now())

        PaymentInLine secondLine = context.newObject(PaymentInLine.class)
        secondLine.setAmount(Money.ZERO)
        secondLine.setPayment(secondPaymentIn)
        secondLine.setInvoice(secondInvoice)
        secondLine.setAccountOut(secondInvoice.getDebtorsAccount())

        PaymentIn secondPaymentInVoucher = context.newObject(PaymentIn.class)
        secondPaymentInVoucher.setStatus(PaymentStatus.SUCCESS)
        secondPaymentInVoucher.setSource(PaymentSource.SOURCE_ONCOURSE)
        SetPaymentMethod.valueOf(PaymentMethodUtil.getVOUCHERPaymentMethods(context, PaymentMethod.class), secondPaymentInVoucher).set()
        secondPaymentInVoucher.setAmount(secondInvoice.getAmountOwing())
        secondPaymentInVoucher.setPayer(secondInvoice.getContact())
        secondPaymentInVoucher.setPaymentDate(LocalDate.now())
        secondPaymentInVoucher.setAccountIn(liabilityAccount)

        PaymentInLine secondLineVoucher = context.newObject(PaymentInLine.class)
        secondLineVoucher.setAmount(secondInvoice.getAmountOwing())
        secondLineVoucher.setPayment(secondPaymentInVoucher)
        secondLineVoucher.setInvoice(secondInvoice)
        secondLineVoucher.setAccountOut(secondInvoice.getDebtorsAccount())

        Voucher secondVoucher = SelectById.query(Voucher.class, 3L).selectOne(context)
        Money balance2 = voucher.getRedemptionValue().subtract(invoice.getAmountOwing())
        voucher.setRedemptionValue(balance2)

        VoucherPaymentIn secondVoucherPaymentIn = context.newObject(VoucherPaymentIn.class)
        secondVoucherPaymentIn.setPaymentIn(secondPaymentInVoucher)
        secondVoucherPaymentIn.setVoucher(secondVoucher)
        secondVoucherPaymentIn.setStatus(VoucherPaymentStatus.APPROVED)
        secondVoucherPaymentIn.setInvoiceLine(secondInvoice.getInvoiceLines().get(0))

        context.commitChanges()

        List<AccountTransaction> secondTransactions = ObjectSelect.query(AccountTransaction.class)
				.where(AccountTransaction.FOREIGN_RECORD_ID.eq(secondLineVoucher.getId()))
				.select(context)
        assertEquals(4, secondTransactions.size())

        List<AccountTransaction> asset2Transactions = AccountTransaction.ACCOUNT.eq(assetAccount).filterObjects(secondTransactions)
        assertEquals(1, asset2Transactions.size())
        assertEquals(new Money("-30.00"), asset2Transactions.get(0).getAmount())

        List<AccountTransaction> liability2Transactions = AccountTransaction.ACCOUNT.eq(liabilityAccount).filterObjects(secondTransactions)
        assertEquals(1, liability2Transactions.size())
        assertEquals(new Money("-30.00"), liability2Transactions.get(0).getAmount())

        List<AccountTransaction> expense2Transactions = AccountTransaction.ACCOUNT.eq(expenseAccount).filterObjects(secondTransactions)
        assertEquals(2, expense2Transactions.size())

        AccountTransaction asset_expense2 = expense2Transactions.get(0).getAmount().isNegative() ? expense2Transactions.get(1) : expense2Transactions.get(0)
        AccountTransaction liability_expense2 = expense2Transactions.get(0).getAmount().isNegative() ? expense2Transactions.get(0) : expense2Transactions.get(1)

        assertEquals(new Money("30.00"), asset_expense2.getAmount())
        assertEquals(new Money("-30.00"), liability_expense2.getAmount())
    }
}
