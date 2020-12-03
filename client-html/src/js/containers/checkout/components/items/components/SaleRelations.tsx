/*
 * Copyright ish group pty ltd. All rights reserved. https://www.ish.com.au
 * No copying or use of this code is allowed without permission in writing from ish.
 */
import React, { useMemo } from "react";
import { Card } from "@material-ui/core";
import makeStyles from "@material-ui/core/styles/makeStyles";
import Grid from "@material-ui/core/Grid";
import { CheckoutItem, CheckoutSaleRelationExtended } from "../../../../../model/checkout";
import { NestedListRow } from "../../../../../common/components/form/nestedList/components/ListRenderer";
import { AppTheme } from "../../../../../model/common/Theme";

interface Props {
  relations: CheckoutSaleRelationExtended[];
  cartItems: CheckoutItem[];
  onSelect?: any;
}

const useStyles = makeStyles((theme: AppTheme) => ({
  listRoot: {
    listStyle: "none",
    padding: theme.spacing(1, 0, 0, 0),
    margin: 0
  }
}));

const SaleRelations: React.FC<Props> = ({ relations, cartItems, onSelect }) => {
  const classes = useStyles();

  const groupedRelations = useMemo(() => {
    const result = {};
    relations.forEach(r => {
      const item = cartItems.find(i => [i.id, i.courseId].includes(r.fromItem.id));

      if (!item || cartItems.some(c => c.id === r.toItem.id || c.courseId === r.toItem.id)) {
        return;
      }

      const name = item.type === "course" ? `${item.code}  ${item.name}` : item.name;
      if (result[name]) {
        result[name].push(r.toItem);
      } else {
        result[name] = [r.toItem];
      }
    });

    return result;
  }, [relations, cartItems]);

  return (
    <Grid container className="p-3">
      <Grid item xs={12} sm={8}>
        {Object.keys(groupedRelations).map(k => (
          <div key={k}>
            <div className="heading pb-1">{k}</div>
            <Card>
              <ul className={classes.listRoot}>
                {groupedRelations[k].map(r => (
                  <NestedListRow
                    key={r.id}
                    type="search"
                    onClick={() => onSelect(r.cartItem, r.type.toLowerCase())}
                    item={{
                      link: r.link,
                      primaryText: r.cartItem.name,
                      secondaryText: r.cartItem.code,
                      entityName: r.type,
                      active: true
                    }}
                  />
                ))}
              </ul>
            </Card>
          </div>
        ))}
      </Grid>
    </Grid>
);
};

export default SaleRelations;
