package ish.oncourse.server.cayenne.changeFilterTests;

import ish.common.types.PaymentStatus;
import ish.common.types.PaymentType;
import ish.oncourse.entity.services.SetPaymentMethod;
import ish.oncourse.server.cayenne.Banking;
import ish.oncourse.server.cayenne.Invoice;
import ish.oncourse.server.cayenne.PaymentMethod;
import ish.oncourse.server.cayenne.PaymentOut;
import ish.oncourse.server.cayenne.PaymentOutLine;
import ish.oncourse.server.lifecycle.BankingChangeHandler;
import ish.oncourse.server.lifecycle.ChangeFilter;
import ish.util.PaymentMethodUtil;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.query.SelectById;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;

/**
 * This class needs to be Java as of October 2019 since groovy doesn't handle the @PreUpdate annotation properly
 */
public class UnbankTest extends ChangeFilterTest {
    @Test
    public void testUnbank() {
        cayenneService.addListener(new Object() {
            @PreUpdate(value = PaymentOut.class)
            public void preUpdate(PaymentOut paymentOut) {

                BankingChangeHandler changeHalper = new BankingChangeHandler(paymentOut.getContext());

                ChangeFilter.preCommitGraphDiff(paymentOut.getObjectContext()).apply(changeHalper);

                Banking oldValue = changeHalper.getOldValueFor(paymentOut.getObjectId());
                Assert.assertEquals(Long.valueOf(100L), oldValue.getId());

                Banking newValue = changeHalper.getNewValueFor(paymentOut.getObjectId());
                Assert.assertNull(newValue);

            }

        });

        DataContext context = cayenneService.getNewContext();

        Invoice invoice = SelectById.query(Invoice.class, 200L).selectOne(context);

        PaymentOut payment = context.newObject(PaymentOut.class);
        PaymentOutLine outLine = context.newObject(PaymentOutLine.class);

        outLine.setPaymentOut(payment);
        outLine.setAccount(invoice.getDebtorsAccount());
        outLine.setInvoice(invoice);
        outLine.setAmount(invoice.getAmountOwing().negate());

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPayee(invoice.getContact());
        payment.setAmount(invoice.getAmountOwing().negate());
        payment.setPaymentDate(LocalDate.now());
        SetPaymentMethod.valueOf(PaymentMethodUtil.getCustomPaymentMethod(context, PaymentMethod.class, PaymentType.CASH), payment).set();

        Banking b1 = SelectById.query(Banking.class, 100L).selectOne(context);
        payment.setBanking(b1);

        context.commitChanges();

        payment.setBanking(null);

        context.commitChanges();
    }

}
